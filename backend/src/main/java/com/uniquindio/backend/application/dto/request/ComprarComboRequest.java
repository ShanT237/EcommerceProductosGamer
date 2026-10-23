package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Positive;

// Mapea a: ComprarComboUseCase.ejecutar(usuarioId, comboId, cantidad)
// El comboId viaja en la URL: POST /api/combos/{id}/comprar
// El usuarioId sale del token (Authentication), no del body.
public record ComprarComboRequest(
        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}