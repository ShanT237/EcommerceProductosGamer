package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.RegaloPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RegaloRepositoryJpa implements RegaloRepository {

    private final RegaloJpaDataRepository dataRepository;
    private final RegaloPersistenceMapper mapper;

    @Override
    public Optional<Regalo> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Regalo regalo) {
        dataRepository.save(mapper.toEntity(regalo));
    }

    @Override
    public long contarAsignadosA(UUID usuarioId) {
        return dataRepository.countByIdUsuarioAndEstadoNot(usuarioId, EstadoRegalo.CANCELADO);
    }
}