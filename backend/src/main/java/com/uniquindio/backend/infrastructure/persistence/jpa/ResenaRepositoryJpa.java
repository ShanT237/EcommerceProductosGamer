package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.repository.ResenaRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ResenaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ResenaRepositoryJpa implements ResenaRepository {

    private final ResenaJpaDataRepository dataRepository;
    private final ResenaPersistenceMapper mapper;

    @Override
    public Optional<Resena> obtenerPorId(String id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Resena resena) {
        dataRepository.save(mapper.toEntity(resena));
    }

    @Override
    public boolean existeResenaDeUsuarioYProducto(String usuarioId, String productoId) {
        return dataRepository.existsByIdUsuarioAndIdProducto(usuarioId, productoId);
    }
}