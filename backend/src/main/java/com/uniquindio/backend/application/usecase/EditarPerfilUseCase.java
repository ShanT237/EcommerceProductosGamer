package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EditarPerfil (CU-26). Cambia nombre y correo; el correo debe seguir siendo
 * único (conservar el propio está permitido).
 */
@Service
@RequiredArgsConstructor
public class EditarPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    public Usuario ejecutar(UUID usuarioId, String nombre, String correo) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
        usuario.asegurarActivo();

        boolean correoDeOtro = usuarioRepository.obtenerPorCorreo(correo)
                .filter(existente -> !existente.getId().equals(usuarioId))
                .isPresent();
        if (correoDeOtro) {
            throw new ReglaDominioException("Ya existe un usuario con ese correo");
        }

        usuario.actualizarPerfil(nombre, correo);

        usuarioRepository.guardar(usuario);

        return usuario;
    }
}
