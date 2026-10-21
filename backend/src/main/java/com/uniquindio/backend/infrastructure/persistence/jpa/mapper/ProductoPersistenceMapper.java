package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductoPersistenceMapper {

    // Dominio -> Entidad JPA (para guardar en la base)
    public ProductoEntity toEntity(Producto producto) {
        ProductoEntity entity = new ProductoEntity();
        entity.setId(producto.getId());
        entity.setVendedorId(producto.getVendedorId());
        entity.setNombre(producto.getNombre());
        entity.setDescripcion(producto.getDescripcion());
        entity.setPrecio(producto.getPrecio());
        entity.setPrecioOriginal(producto.getPrecioOriginal());
        entity.setStock(producto.getStock());
        entity.setGama(producto.getGama());
        entity.setExclusividad(producto.getExclusividad());
        entity.setEliminado(producto.isEliminado());
        entity.setPausado(producto.isPausado());
        return entity;
    }

    // Entidad JPA -> Dominio (al leer de la base)
    public Producto toDomain(ProductoEntity entity) {
        return Producto.reconstruir(
                entity.getId(),
                entity.getVendedorId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getPrecio(),
                entity.getStock(),
                entity.getGama(),
                entity.getExclusividad(),
                entity.isEliminado(),
                entity.getPrecioOriginal(),
                entity.isPausado());
    }
}