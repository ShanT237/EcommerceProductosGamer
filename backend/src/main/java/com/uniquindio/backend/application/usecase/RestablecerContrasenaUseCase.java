package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.HashToken;
import com.uniquindio.backend.domain.entity.TokenRecuperacion;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.TokenRecuperacionRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.service.HashContrasena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Caso de uso: RestablecerContrasena (seguridad).
 * Cambia la contraseña de la cuenta dueña del token. El token se consume solo si la contraseña
 * nueva es válida: si es débil, se rechaza y el enlace sigue sirviendo para reintentar.
 * Un enlace inexistente, vencido o ya usado produce siempre el mismo error.
 */
@Service
@RequiredArgsConstructor
public class RestablecerContrasenaUseCase {

    static final String ENLACE_INVALIDO = "El enlace no es válido o ya venció. Solicita uno nuevo.";

    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final HashContrasena hashContrasena;
    private final Clock clock;

    @Transactional
    public void ejecutar(String tokenPlano, String contrasenaNueva) {
        if (tokenPlano == null || tokenPlano.isBlank()) {
            throw new ReglaDominioException(ENLACE_INVALIDO);
        }
        Instant ahora = Instant.now(clock);
        TokenRecuperacion token = tokenRepository.obtenerPorHash(HashToken.hashear(tokenPlano.trim()))
                .filter(t -> t.estaVigente(ahora))
                .orElseThrow(() -> new ReglaDominioException(ENLACE_INVALIDO));

        if (token.getRol().esVendedor()) {
            restablecerVendedor(token.getCuentaId(), contrasenaNueva);
        } else {
            restablecerComprador(token.getCuentaId(), contrasenaNueva);
        }
        tokenRepository.eliminarDeCuenta(token.getCuentaId());
    }

    private void restablecerComprador(UUID id, String contrasenaNueva) {
        Usuario usuario = usuarioRepository.obtenerPorId(id)
                .orElseThrow(() -> new ReglaDominioException(ENLACE_INVALIDO));
        usuario.asegurarActivo();
        usuario.establecerContrasenaSegura(contrasenaNueva, hashContrasena);
        usuarioRepository.guardar(usuario);
    }

    private void restablecerVendedor(UUID id, String contrasenaNueva) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(id)
                .orElseThrow(() -> new ReglaDominioException(ENLACE_INVALIDO));
        vendedor.asegurarActivo();
        vendedor.establecerContrasenaSegura(contrasenaNueva, hashContrasena);
        vendedorRepository.guardar(vendedor);
    }
}
