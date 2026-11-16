package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Consulta el perfil del usuario autenticado (CU-26, lectura). */
@Service
@RequiredArgsConstructor
public class ObtenerPerfilUseCase {

    private final UsuarioRepository usuarioRepository;

    public Usuario ejecutar(UUID usuarioId) {
        return usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"));
    }
}
