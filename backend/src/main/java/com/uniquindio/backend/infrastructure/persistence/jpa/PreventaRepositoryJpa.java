package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.PreventaPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PreventaRepositoryJpa implements PreventaRepository {

    private final PreventaJpaDataRepository dataRepository;
    private final PreventaPersistenceMapper mapper;

    @Override
    public Optional<Preventa> obtenerPorProductoId(UUID productoId) {
        return dataRepository.findByProductoId(productoId).map(mapper::toDomain);
    }

    @Override
    public List<Preventa> obtenerFuturas(LocalDate hoy) {
        return dataRepository.findByFechaLanzamientoAfter(hoy).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void guardar(Preventa preventa) {
        dataRepository.save(mapper.toEntity(preventa));
    }
}