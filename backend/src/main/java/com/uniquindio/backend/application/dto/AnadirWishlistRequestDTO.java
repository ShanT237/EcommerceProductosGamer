package com.uniquindio.backend.application.dto;

import java.util.UUID;

public record AnadirWishlistRequestDTO(
        UUID wishlistId,
        UUID usuarioId,
        UUID productoId
) {
}