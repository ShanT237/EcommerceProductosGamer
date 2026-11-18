package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.TokenRecuperacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.security.BCryptHashContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RecuperacionContrasenaUseCaseTest {

    /** Reloj que se puede adelantar para probar vencimientos. */
    private static class RelojMutable extends Clock {
        private Instant ahora = Instant.parse("2026-11-17T15:00:00Z");

        void avanzar(Duration lapso) {
            ahora = ahora.plus(lapso);
        }

        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_-]+)");

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private TokenRecuperacionRepositoryEnMemoria tokenRepository;
    private BCryptHashContrasena hasher;
    private RelojMutable reloj;
    private List<String[]> correos;
    private SolicitarRecuperacionContrasenaUseCase solicitar;
    private RestablecerContrasenaUseCase restablecer;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        tokenRepository = new TokenRecuperacionRepositoryEnMemoria();
        hasher = new BCryptHashContrasena();
        reloj = new RelojMutable();
        correos = new ArrayList<>();
        solicitar = new SolicitarRecuperacionContrasenaUseCase(usuarioRepository, vendedorRepository,
                tokenRepository, (destino, asunto, html) -> correos.add(new String[]{destino, asunto, html}),
                reloj, "https://tienda.test/");
        restablecer = new RestablecerContrasenaUseCase(usuarioRepository, vendedorRepository,
                tokenRepository, hasher, reloj);

        usuarioId = UUID.randomUUID();
        new RegistrarUsuarioUseCase(usuarioRepository, hasher).ejecutar(usuarioId, "Gamer", "g@test.com", "Gamer2026");
    }

    private String tokenDelUltimoCorreo() {
        Matcher m = TOKEN.matcher(correos.get(correos.size() - 1)[2]);
        assertTrue(m.find(), "el correo debe traer el enlace con el token");
        return m.group(1);
    }

    private Usuario usuario() {
        return usuarioRepository.obtenerPorId(usuarioId).get();
    }

    @Test
    void correoRegistradoRecibeUnEnlaceHaciaElFrontend() {
        solicitar.ejecutar("g@test.com");

        assertEquals(1, correos.size());
        assertEquals("g@test.com", correos.get(0)[0]);
        assertTrue(correos.get(0)[2].contains("https://tienda.test/restablecer?token="));
    }

    @Test
    void correoInexistenteNoEnviaNadaNiFalla() {
        assertDoesNotThrow(() -> solicitar.ejecutar("nadie@test.com"));

        assertTrue(correos.isEmpty());
        assertEquals(0, tokenRepository.cantidad());
    }

    @Test
    void cuentaDadaDeBajaNoRecibeCorreo() {
        usuario().darDeBaja();

        solicitar.ejecutar("g@test.com");

        assertTrue(correos.isEmpty());
    }

    @Test
    void solicitarEnRafagaSoloEnviaUnCorreoYDespuesDeUnMinutoInvalidaElAnterior() {
        solicitar.ejecutar("g@test.com");
        String primero = tokenDelUltimoCorreo();
        solicitar.ejecutar("g@test.com");
        assertEquals(1, correos.size());

        reloj.avanzar(Duration.ofSeconds(61));
        solicitar.ejecutar("g@test.com");
        assertEquals(2, correos.size());
        String segundo = tokenDelUltimoCorreo();

        assertNotEquals(primero, segundo);
        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar(primero, "Nueva2027x"));
        assertDoesNotThrow(() -> restablecer.ejecutar(segundo, "Nueva2027x"));
    }

    @Test
    void conElEnlaceSePuedeElegirUnaContrasenaNuevaYSoloUnaVez() {
        solicitar.ejecutar("g@test.com");
        String token = tokenDelUltimoCorreo();

        restablecer.ejecutar(token, "Nueva2027x");

        assertTrue(usuario().verificarContrasena("Nueva2027x", hasher));
        assertFalse(usuario().verificarContrasena("Gamer2026", hasher));
        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar(token, "Otra2028xyz"));
    }

    @Test
    void enlaceVencidoSeRechazaYNoCambiaLaContrasena() {
        solicitar.ejecutar("g@test.com");
        String token = tokenDelUltimoCorreo();

        reloj.avanzar(Duration.ofMinutes(31));

        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar(token, "Nueva2027x"));
        assertTrue(usuario().verificarContrasena("Gamer2026", hasher));
    }

    @Test
    void tokenInventadoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar("inventado", "Nueva2027x"));
        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar("  ", "Nueva2027x"));
    }

    @Test
    void contrasenaDebilSeRechazaYElEnlaceSigueSirviendoParaReintentar() {
        solicitar.ejecutar("g@test.com");
        String token = tokenDelUltimoCorreo();

        assertThrows(ReglaDominioException.class, () -> restablecer.ejecutar(token, "debil"));
        assertTrue(usuario().verificarContrasena("Gamer2026", hasher));

        assertDoesNotThrow(() -> restablecer.ejecutar(token, "Nueva2027x"));
        assertTrue(usuario().verificarContrasena("Nueva2027x", hasher));
    }

    @Test
    void tambienFuncionaParaVendedores() {
        UUID vendedorId = UUID.randomUUID();
        Vendedor vendedor = Vendedor.registrar(vendedorId, "Tienda", "v@test.com", TipoVendedor.PERSONA_NATURAL, "123456");
        vendedor.establecerContrasenaSegura("Tienda2026", hasher);
        vendedorRepository.guardar(vendedor);

        solicitar.ejecutar("v@test.com");
        restablecer.ejecutar(tokenDelUltimoCorreo(), "Nueva2027x");

        assertTrue(vendedorRepository.obtenerPorId(vendedorId).get().verificarContrasena("Nueva2027x", hasher));
    }
}
