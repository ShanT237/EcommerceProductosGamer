package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Positive;

// Mapea a: ModificarProductoUseCase.ejecutar(productoId, vendedorId, nuevoPrecio)
// El vendedorId sale del token (Authentication), no del body.
public record ModificarProductoRequest(
        @Positive(message = "El precio debe ser mayor a cero")
        double nuevoPrecio
) {}