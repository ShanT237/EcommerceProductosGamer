package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record QuitarWishlistRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @NotNull(message = "El producto es obligatorio")
        UUID productoId
) {}