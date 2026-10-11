package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Wishlist;

import java.util.List;
import java.util.UUID;

public record AnadirWishlistResponse(
        UUID id,
        UUID usuarioId,
        List<UUID> productos
) {
    public static AnadirWishlistResponse desde(Wishlist wishlist) {
        return new AnadirWishlistResponse(
                wishlist.getId(),
                wishlist.getUsuarioId(),
                wishlist.getProductos()
        );
    }
}