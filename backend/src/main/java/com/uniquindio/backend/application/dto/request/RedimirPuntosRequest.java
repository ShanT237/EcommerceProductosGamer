package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record RedimirPuntosRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @Positive(message = "La cantidad de puntos a redimir debe ser mayor a cero")
        int cantidad
) {}