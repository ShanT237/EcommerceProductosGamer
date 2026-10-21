package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.UsuarioRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UsuarioRepositoryEnMemoria implements UsuarioRepository {

    private final Map<UUID, Usuario> usuarios = new HashMap<>();

    @Override
    public Optional<Usuario> obtenerPorId(UUID id) {
        return Optional.ofNullable(usuarios.get(id));
    }

    @Override
    public void guardar(Usuario usuario) {
        usuarios.put(usuario.getId(), usuario);
    }

    @Override
    public boolean existeConCorreo(String correo) {
        return usuarios.values().stream()
                .anyMatch(u -> u.getCorreo().equals(correo));
    }

    @Override
    public Optional<Usuario> obtenerPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return usuarios.values().stream()
                .filter(u -> u.getCorreo().equalsIgnoreCase(correo.trim()))
                .findFirst();
    }
}
