package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.WishlistEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class WishlistPersistenceMapper {

    public WishlistEntity toEntity(Wishlist wishlist) {
        WishlistEntity entity = new WishlistEntity();
        entity.setId(wishlist.getId());
        entity.setUsuarioId(wishlist.getUsuarioId());
        entity.setProductos(new ArrayList<>(wishlist.getProductos()));
        return entity;
    }

    public Wishlist toDomain(WishlistEntity entity) {
        return Wishlist.reconstruir(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getProductos()
        );
    }
}