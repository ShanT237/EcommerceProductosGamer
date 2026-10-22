package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.repository.MovimientoPuntosRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.MovimientoPuntosPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MovimientoPuntosRepositoryJpa implements MovimientoPuntosRepository {

    private final MovimientoPuntosJpaDataRepository dataRepository;
    private final MovimientoPuntosPersistenceMapper mapper;

    @Override
    public void guardar(MovimientoPuntos movimiento) {
        dataRepository.save(mapper.toEntity(movimiento));
    }

    @Override
    public List<MovimientoPuntos> obtenerPorUsuarioId(UUID usuarioId) {
        return dataRepository.findByUsuarioIdOrderByFechaAsc(usuarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}