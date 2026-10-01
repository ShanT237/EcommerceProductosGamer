package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: Regalo.elegir()
public record ElegirRegaloRequest(
        @NotNull(message = "El regalo es obligatorio")
        UUID regaloId,

        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId
) {}