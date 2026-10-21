package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.UsuarioPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class UsuarioRepositoryJpa implements UsuarioRepository {

    private final UsuarioJpaDataRepository dataRepository;
    private final UsuarioPersistenceMapper mapper;

    @Override
    public Optional<Usuario> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Usuario usuario) {
        dataRepository.save(mapper.toEntity(usuario));
    }

    @Override
    public boolean existeConCorreo(String correo) {
        return dataRepository.existsByCorreo(correo);
    }
}