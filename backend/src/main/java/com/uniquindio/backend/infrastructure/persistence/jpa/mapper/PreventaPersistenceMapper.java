package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PreventaEntity;
import org.springframework.stereotype.Component;

@Component
public class PreventaPersistenceMapper {

    public PreventaEntity toEntity(Preventa preventa) {
        PreventaEntity entity = new PreventaEntity();
        entity.setId(preventa.getId());
        entity.setProductoId(preventa.getProductoId());
        entity.setFechaLanzamiento(preventa.getFechaLanzamiento());
        entity.setDescripcion(preventa.getDescripcion());
        return entity;
    }

    public Preventa toDomain(PreventaEntity entity) {
        return Preventa.reconstruir(
                entity.getId(),
                entity.getProductoId(),
                entity.getFechaLanzamiento(),
                entity.getDescripcion()
        );
    }
}