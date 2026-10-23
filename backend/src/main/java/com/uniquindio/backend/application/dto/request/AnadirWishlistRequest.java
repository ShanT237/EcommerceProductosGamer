package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: AnadirAWishlistUseCase.ejecutar(usuarioId, productoId)
// El usuarioId sale del token (Authentication), no del body.
public record AnadirWishlistRequest(
        @NotNull(message = "El id de la wishlist es obligatorio")
        UUID wishlistId,

        @NotNull(message = "El producto es obligatorio")
        UUID productoId
) {}