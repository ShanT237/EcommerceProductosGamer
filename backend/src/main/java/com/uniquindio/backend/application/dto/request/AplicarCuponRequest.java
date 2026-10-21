package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: AplicarCuponUseCase.ejecutar(compraId, usuarioId, codigo)
// El compraId viaja en la URL: PUT /api/compras/{id}/cupon
public record AplicarCuponRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @NotBlank(message = "El código del cupón es obligatorio")
        String codigo
) {}