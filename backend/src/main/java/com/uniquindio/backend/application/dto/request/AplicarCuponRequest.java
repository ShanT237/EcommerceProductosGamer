package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;

// Mapea a: AplicarCuponUseCase.ejecutar(compraId, usuarioId, codigo)
// El compraId viaja en la URL: PUT /api/compras/{id}/cupon
// El usuarioId sale del token (Authentication), no del body.
public record AplicarCuponRequest(
        @NotBlank(message = "El código del cupón es obligatorio")
        String codigo
) {}