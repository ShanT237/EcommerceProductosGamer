package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ValoracionVendedorEntity;
import org.springframework.stereotype.Component;

@Component
public class ValoracionVendedorPersistenceMapper {

    public ValoracionVendedorEntity toEntity(ValoracionVendedor valoracion) {
        ValoracionVendedorEntity entity = new ValoracionVendedorEntity();
        entity.setId(valoracion.getId());
        entity.setCompraId(valoracion.getCompraId());
        entity.setUsuarioId(valoracion.getUsuarioId());
        entity.setVendedorId(valoracion.getVendedorId());
        entity.setCalificacion(valoracion.getCalificacion().estrellas());
        entity.setComentario(valoracion.getComentario());
        entity.setFecha(valoracion.getFecha());
        return entity;
    }

    public ValoracionVendedor toDomain(ValoracionVendedorEntity entity) {
        return ValoracionVendedor.reconstruir(
                entity.getId(),
                entity.getCompraId(),
                entity.getUsuarioId(),
                entity.getVendedorId(),
                new Calificacion(entity.getCalificacion()),
                entity.getComentario(),
                entity.getFecha());
    }
}