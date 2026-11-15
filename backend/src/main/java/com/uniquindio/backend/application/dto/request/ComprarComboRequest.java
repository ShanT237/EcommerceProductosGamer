package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Mapea a: ComprarComboUseCase.ejecutar(usuarioId, comboId, cantidad)
// El comboId viaja en la URL: POST /api/combos/{id}/comprar
// El usuarioId sale del token (Authentication), no del body.
public record ComprarComboRequest(
        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad,
        @Size(max = 50, message = "El código del cupón admite como máximo 50 caracteres")
        String codigoCupon
) {}