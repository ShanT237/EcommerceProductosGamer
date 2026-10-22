package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CuponEntity;
import org.springframework.stereotype.Component;

@Component
public class CuponPersistenceMapper {

    public CuponEntity toEntity(Cupon cupon) {
        CuponEntity entity = new CuponEntity();
        entity.setId(cupon.getId());
        entity.setCodigo(cupon.getCodigo());
        entity.setPorcentaje(cupon.getPorcentaje());
        entity.setVence(cupon.getVence());
        entity.setUsosMaximos(cupon.getUsosMaximos());
        entity.setUsos(cupon.getUsos());
        return entity;
    }

    public Cupon toDomain(CuponEntity entity) {
        return Cupon.reconstruir(
                entity.getId(),
                entity.getCodigo(),
                entity.getPorcentaje(),
                entity.getVence(),
                entity.getUsosMaximos(),
                entity.getUsos());
    }
}