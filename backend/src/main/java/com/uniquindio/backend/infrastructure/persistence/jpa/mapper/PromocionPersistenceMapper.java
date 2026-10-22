package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PromocionEntity;
import org.springframework.stereotype.Component;

@Component
public class PromocionPersistenceMapper {

    public PromocionEntity toEntity(Promocion promocion) {
        PromocionEntity entity = new PromocionEntity();
        entity.setId(promocion.getId());
        entity.setProductoId(promocion.getProductoId());
        entity.setPorcentaje(promocion.getPorcentaje());
        entity.setDesde(promocion.getDesde());
        entity.setHasta(promocion.getHasta());
        return entity;
    }

    public Promocion toDomain(PromocionEntity entity) {
        return Promocion.reconstruir(
                entity.getId(),
                entity.getProductoId(),
                entity.getPorcentaje(),
                entity.getDesde(),
                entity.getHasta());
    }
}