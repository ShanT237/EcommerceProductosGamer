package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: Wishlist.agregarProducto()
public record AnadirWishlistRequest(
        @NotNull(message = "El id de la wishlist es obligatorio")
        UUID wishlistId,

        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @NotNull(message = "El producto es obligatorio")
        UUID productoId
) {}