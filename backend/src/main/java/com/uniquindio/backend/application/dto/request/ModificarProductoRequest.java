package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// Mapea a: ModificarProductoUseCase.ejecutar(productoId, vendedorId, nuevoPrecio)
public record ModificarProductoRequest(
        @NotNull(message = "El vendedor es obligatorio")
        UUID vendedorId,

        @Positive(message = "El precio debe ser mayor a cero")
        double nuevoPrecio
) {}