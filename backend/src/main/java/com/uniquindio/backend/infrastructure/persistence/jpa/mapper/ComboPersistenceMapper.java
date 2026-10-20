package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ComboEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ComboPersistenceMapper {

    public ComboEntity toEntity(Combo combo) {
        ComboEntity entity = new ComboEntity();
        entity.setId(combo.getId());
        entity.setVendedorId(combo.getVendedorId());
        entity.setNombre(combo.getNombre());
        entity.setDescripcion(combo.getDescripcion());
        entity.setDescuento(combo.getDescuento());
        entity.setProductos(new ArrayList<>(combo.getProductos()));
        return entity;
    }

    public Combo toDomain(ComboEntity entity) {
        return Combo.reconstruir(
                entity.getId(),
                entity.getVendedorId(),
                entity.getNombre(),
                entity.getDescripcion(),
                entity.getDescuento(),
                entity.getProductos()
        );
    }
}