package com.uniquindio.backend.application.dto;

import com.uniquindio.backend.domain.entity.Compra;

import java.time.LocalDate;
import java.util.UUID;

public record RealizarCompraResponseDTO(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        int cantidad,
        double precioUnitario,
        double subtotal,
        LocalDate fecha
) {
    public static RealizarCompraResponseDTO desde(Compra compra) {
        return new RealizarCompraResponseDTO(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getProductoId(),
                compra.getCantidad(),
                compra.getPrecioUnitario().valor(),
                compra.getSubtotal().valor(),
                compra.getFecha()
        );
    }
}