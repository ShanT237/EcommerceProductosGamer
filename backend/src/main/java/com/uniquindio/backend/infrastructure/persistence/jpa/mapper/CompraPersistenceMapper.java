package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CompraEntity;
import org.springframework.stereotype.Component;

@Component
public class CompraPersistenceMapper {

    public CompraEntity toEntity(Compra compra) {
        CompraEntity entity = new CompraEntity();
        entity.setId(compra.getId());
        entity.setUsuarioId(compra.getUsuarioId());
        entity.setProductoId(compra.getProductoId());
        entity.setCantidad(compra.getCantidad());
        entity.setFecha(compra.getFecha());
        entity.setPrecioUnitario(compra.getPrecioUnitario().valor());
        entity.setEstado(compra.getEstado());
        entity.setResenada(compra.isResenada());
        entity.setPuntosAsignados(compra.isPuntosAsignados());
        entity.setDescargado(compra.isDescargado());
        entity.setCuponAplicado(compra.isCuponAplicado());
        return entity;
    }

    public Compra toDomain(CompraEntity entity) {
        return Compra.reconstruir(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getProductoId(),
                entity.getCantidad(),
                entity.getFecha(),
                new Precio(entity.getPrecioUnitario()),
                entity.getEstado(),
                entity.isResenada(),
                entity.isPuntosAsignados(),
                entity.isDescargado(),
                entity.isCuponAplicado());
    }
}