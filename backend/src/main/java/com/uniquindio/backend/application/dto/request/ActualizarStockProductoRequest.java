package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Min;

// Mapea a: ActualizarStockProductoUseCase.ejecutar(productoId, vendedorId, nuevoStock)
// El vendedorId sale del token (Authentication), no del body.
public record ActualizarStockProductoRequest(
        @Min(value = 0, message = "El stock no puede ser negativo")
        int stock
) {}
