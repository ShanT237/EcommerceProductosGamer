package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// Mapea a: RealizarCompraUseCase.ejecutar(usuarioId, productoId, cantidad)
// El usuarioId sale del token (Authentication), no del body.
public record RealizarCompraRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}