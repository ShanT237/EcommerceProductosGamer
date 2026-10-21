package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: RegistrarUsuario (CU-11).
 * El correo del comprador debe ser único.
 */
@Service
@RequiredArgsConstructor
public class RegistrarUsuarioUseCase {

    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Usuario ejecutar(UUID id, String nombre, String correo) {
        if (usuarioRepository.existeConCorreo(correo)) {
            throw new ReglaDominioException("Ya existe un usuario con ese correo");
        }

        Usuario usuario = Usuario.registrar(id, nombre, correo);

        usuarioRepository.guardar(usuario);

        return usuario;
    }
}
