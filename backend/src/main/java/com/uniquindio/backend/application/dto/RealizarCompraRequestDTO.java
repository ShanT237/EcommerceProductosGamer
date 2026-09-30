package com.uniquindio.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record RealizarCompraRequestDTO(
        UUID id,

        @NotNull(message = "El id del usuario es obligatorio")
        UUID usuarioId,

        @NotNull(message = "El id del producto es obligatorio")
        UUID productoId,

        @Min(value = 1, message = "La cantidad debe ser al menos 1")
        int cantidad,

        LocalDate fecha
) {
}