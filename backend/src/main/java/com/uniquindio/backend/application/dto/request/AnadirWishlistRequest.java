package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: AnadirAWishlistUseCase.ejecutar(usuarioId, productoId)
// El usuarioId sale del token (Authentication), no del body.
// La wishlist se resuelve por el usuario del token: no se envía su id.
public record AnadirWishlistRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productoId
) {}