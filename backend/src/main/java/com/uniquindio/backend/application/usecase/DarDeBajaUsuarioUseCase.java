package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: DarDeBajaUsuario (CU-25, comprador).
 * Baja lógica del comprador; conserva su historial de compras, pedidos y reseñas.
 * Una cuenta dada de baja ya no puede iniciar sesión ni operar.
 */
@Service
@RequiredArgsConstructor
public class DarDeBajaUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    public Usuario ejecutar(UUID usuarioId) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));

        usuario.darDeBaja();

        usuarioRepository.guardar(usuario);

        return usuario;
    }
}
