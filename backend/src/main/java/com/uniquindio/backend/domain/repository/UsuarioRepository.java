package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Usuario;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository {
    Optional<Usuario> obtenerPorId(UUID id);
    void guardar(Usuario usuario);
}
