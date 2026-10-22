package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// Mapea a: ComprarComboUseCase.ejecutar(usuarioId, comboId, cantidad)
// El comboId viaja en la URL: POST /api/combos/{id}/comprar
public record ComprarComboRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}