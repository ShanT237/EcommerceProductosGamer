package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Combo;
import java.util.List;
import java.util.UUID;

public record ComboResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        String descripcion,
        double descuento,
        List<UUID> productos
) {
    public static ComboResponse desde(Combo combo) {
        return new ComboResponse(
                combo.getId(),
                combo.getVendedorId(),
                combo.getNombre(),
                combo.getDescripcion(),
                combo.getDescuento(),
                combo.getProductos()
        );
    }
}