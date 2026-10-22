package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MetaRecompensaEntity;
import org.springframework.stereotype.Component;

@Component
public class MetaRecompensaPersistenceMapper {

    public MetaRecompensaEntity toEntity(MetaRecompensa meta) {
        MetaRecompensaEntity entity = new MetaRecompensaEntity();
        entity.setId(meta.getId());
        entity.setComprasRequeridas(meta.getComprasRequeridas());
        entity.setProductoRegaloId(meta.getProductoRegaloId());
        entity.setActiva(meta.isActiva());
        return entity;
    }

    public MetaRecompensa toDomain(MetaRecompensaEntity entity) {
        return MetaRecompensa.reconstruir(
                entity.getId(),
                entity.getComprasRequeridas(),
                entity.getProductoRegaloId(),
                entity.isActiva());
    }
}