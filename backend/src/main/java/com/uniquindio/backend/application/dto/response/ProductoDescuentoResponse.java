package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Producto;
import java.util.UUID;

public record ProductoDescuentoResponse(
        UUID id,
        String nombre,
        double nuevoPrecio,           // precio vigente tras aplicar (o quitar) el descuento
        Double precioOriginal,        // null cuando ya no hay descuento
        Double porcentajeDescuento    // null cuando ya no hay descuento
) {
    public static ProductoDescuentoResponse desde(Producto producto) {
        return new ProductoDescuentoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getPrecioOriginal(),
                producto.getPorcentajeDescuento()
        );
    }
}
