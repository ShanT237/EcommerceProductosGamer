package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.port.out.EnviadorCorreos;
import com.uniquindio.backend.application.service.HashToken;
import com.uniquindio.backend.domain.entity.TokenRecuperacion;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.TokenRecuperacionRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.Rol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: SolicitarRecuperacionContrasena (seguridad).
 * Si el correo pertenece a una cuenta activa (comprador o vendedor), le envía un enlace de un solo
 * uso para elegir una contraseña nueva. NUNCA revela si el correo existe: para el llamador, la
 * operación termina igual en ambos casos. Pedir enlaces en ráfaga se ignora (un correo por minuto
 * por cuenta) y un enlace nuevo invalida los anteriores.
 */
@Service
public class SolicitarRecuperacionContrasenaUseCase {

    static final Duration VIGENCIA = Duration.ofMinutes(30);
    static final Duration ESPERA_ENTRE_ENVIOS = Duration.ofSeconds(60);

    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final EnviadorCorreos enviadorCorreos;
    private final Clock clock;
    private final String urlFrontend;

    public SolicitarRecuperacionContrasenaUseCase(
            UsuarioRepository usuarioRepository,
            VendedorRepository vendedorRepository,
            TokenRecuperacionRepository tokenRepository,
            EnviadorCorreos enviadorCorreos,
            Clock clock,
            @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend) {
        this.usuarioRepository = usuarioRepository;
        this.vendedorRepository = vendedorRepository;
        this.tokenRepository = tokenRepository;
        this.enviadorCorreos = enviadorCorreos;
        this.clock = clock;
        this.urlFrontend = urlFrontend.endsWith("/")
                ? urlFrontend.substring(0, urlFrontend.length() - 1)
                : urlFrontend;
    }

    @Transactional
    public void ejecutar(String correo) {
        if (correo == null || correo.isBlank()) {
            return;
        }
        Optional<CuentaRecuperable> encontrada = buscarCuenta(correo.trim());
        if (encontrada.isEmpty()) {
            return;
        }
        CuentaRecuperable cuenta = encontrada.get();
        Instant ahora = Instant.now(clock);

        boolean demasiadoPronto = tokenRepository.obtenerUltimoDeCuenta(cuenta.id())
                .map(t -> t.emitidoHaceMenosDe(ESPERA_ENTRE_ENVIOS, ahora))
                .orElse(false);
        if (demasiadoPronto) {
            return;
        }

        tokenRepository.eliminarDeCuenta(cuenta.id());
        String tokenPlano = HashToken.generar();
        tokenRepository.guardar(TokenRecuperacion.emitir(
                UUID.randomUUID(), HashToken.hashear(tokenPlano), cuenta.id(), cuenta.rol(), ahora, VIGENCIA));

        String enlace = urlFrontend + "/restablecer?token=" + tokenPlano;
        enviadorCorreos.enviar(cuenta.correo(), "Restablece tu contraseña · GamerZone",
                plantillaCorreo(cuenta.nombre(), enlace));
    }

    private Optional<CuentaRecuperable> buscarCuenta(String correo) {
        Optional<CuentaRecuperable> comprador = usuarioRepository.obtenerPorCorreo(correo)
                .filter(Usuario::isActivo)
                .map(u -> new CuentaRecuperable(u.getId(), Rol.COMPRADOR, u.getNombre(), u.getCorreo()));
        if (comprador.isPresent()) {
            return comprador;
        }
        return vendedorRepository.obtenerPorCorreo(correo)
                .filter(Vendedor::isActivo)
                .map(v -> new CuentaRecuperable(v.getId(), Rol.VENDEDOR, v.getNombre(), v.getCorreo()));
    }

    private static String plantillaCorreo(String nombre, String enlace) {
        String saludo = HtmlUtils.htmlEscape(nombre);
        String url = HtmlUtils.htmlEscape(enlace);
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#1c1b29">
                <h2 style="margin:0 0 12px">Hola, %s</h2>
                <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta de GamerZone.</p>
                <p style="margin:24px 0"><a href="%s" style="background:#22e4ff;color:#07060f;padding:12px 22px;border-radius:8px;text-decoration:none;font-weight:bold">Crear nueva contraseña</a></p>
                <p>El enlace vence en 30 minutos y solo se puede usar una vez.</p>
                <p style="font-size:13px;color:#555">Si el botón no funciona, copia y pega esta dirección en tu navegador:<br>%s</p>
                <p style="font-size:13px;color:#555">Si no fuiste tú, ignora este correo: tu contraseña no cambiará.</p>
                </div>
                """.formatted(saludo, url, url);
    }

    private record CuentaRecuperable(UUID id, Rol rol, String nombre, String correo) {}
}
