package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.MovimientoPuntosRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: RedimirPuntos (CU-08).
 * Canjea puntos del usuario; el saldo lo valida la entidad.
 */
@Service
@RequiredArgsConstructor
public class RedimirPuntosUseCase {

    private final UsuarioRepository usuarioRepository;
    private final MovimientoPuntosRepository movimientoRepository;
    private final Clock clock;

    @Transactional
    public Usuario ejecutar(UUID usuarioId, int cantidad) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
        usuario.asegurarActivo();

        usuario.redimirPuntos(cantidad);

        usuarioRepository.guardar(usuario);
        movimientoRepository.guardar(MovimientoPuntos.redimido(usuarioId, cantidad, LocalDateTime.now(clock)));

        return usuario;
    }
}
