package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Producto;

import java.util.UUID;

// Usado en: GET /api/reportes/stock-bajo
public record ProductoStockBajoResponse(
        UUID productoId,
        String nombre,
        int stock,
        boolean agotado
) {
    public static ProductoStockBajoResponse desde(Producto producto) {
        return new ProductoStockBajoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getStock(),
                producto.getStock() == 0
        );
    }
}