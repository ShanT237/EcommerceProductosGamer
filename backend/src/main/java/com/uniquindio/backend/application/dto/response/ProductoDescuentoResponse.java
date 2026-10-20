package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Producto;
import java.util.UUID;

public record ProductoDescuentoResponse(
        UUID id,
        String nombre,
        double nuevoPrecio
) {
    public static ProductoDescuentoResponse desde(Producto producto) {
        return new ProductoDescuentoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio()
        );
    }
}