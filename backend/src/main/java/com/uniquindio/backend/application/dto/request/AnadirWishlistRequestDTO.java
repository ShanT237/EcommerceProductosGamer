package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnadirWishlistRequestDTO(
        UUID wishlistId,

        @NotNull(message = "El id del usuario es obligatorio")
        UUID usuarioId,

        @NotNull(message = "El id del producto es obligatorio")
        UUID productoId
) {
}