package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.service.HashContrasena;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CambiarContrasena (CU-26, seguridad).
 * Cambio directo exigiendo la contraseña actual; la nueva debe ser segura y distinta.
 */
@Service
@RequiredArgsConstructor
public class CambiarContrasenaUseCase {

    private final UsuarioRepository usuarioRepository;
    private final HashContrasena hashContrasena;

    public void ejecutar(UUID usuarioId, String contrasenaActual, String contrasenaNueva) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
        usuario.cambiarContrasena(contrasenaActual, contrasenaNueva, hashContrasena);

        usuarioRepository.guardar(usuario);
    }
}
