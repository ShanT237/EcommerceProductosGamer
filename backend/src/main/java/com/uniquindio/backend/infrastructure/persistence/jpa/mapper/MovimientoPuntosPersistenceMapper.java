package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MovimientoPuntosEntity;
import org.springframework.stereotype.Component;

@Component
public class MovimientoPuntosPersistenceMapper {

    public MovimientoPuntosEntity toEntity(MovimientoPuntos movimiento) {
        MovimientoPuntosEntity entity = new MovimientoPuntosEntity();
        entity.setId(movimiento.getId());
        entity.setUsuarioId(movimiento.getUsuarioId());
        entity.setCantidad(movimiento.getCantidad());
        entity.setTipo(movimiento.getTipo());
        entity.setFecha(movimiento.getFecha());
        return entity;
    }

    public MovimientoPuntos toDomain(MovimientoPuntosEntity entity) {
        return MovimientoPuntos.reconstruir(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getCantidad(),
                entity.getTipo(),
                entity.getFecha());
    }
}