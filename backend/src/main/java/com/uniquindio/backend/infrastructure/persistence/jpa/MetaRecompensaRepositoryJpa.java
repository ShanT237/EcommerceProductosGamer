package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.repository.MetaRecompensaRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.MetaRecompensaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MetaRecompensaRepositoryJpa implements MetaRecompensaRepository {

    private final MetaRecompensaJpaDataRepository dataRepository;
    private final MetaRecompensaPersistenceMapper mapper;

    @Override
    public Optional<MetaRecompensa> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(MetaRecompensa meta) {
        dataRepository.save(mapper.toEntity(meta));
    }

    @Override
    public List<MetaRecompensa> obtenerActivas() {
        return dataRepository.findByActivaTrue().stream()
                .map(mapper::toDomain)
                .toList();
    }
}