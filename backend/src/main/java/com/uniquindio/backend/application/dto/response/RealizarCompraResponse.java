package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;

import java.time.LocalDate;
import java.util.UUID;

public record RealizarCompraResponse(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        int cantidad,
        double precioUnitario,
        double subtotal,
        LocalDate fecha,
        EstadoCompra estado
) {
    public static RealizarCompraResponse desde(Compra compra) {
        return new RealizarCompraResponse(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getProductoId(),
                compra.getCantidad(),
                compra.getPrecioUnitario().valor(),
                compra.getSubtotal().valor(),
                compra.getFecha(),
                compra.getEstado()
        );
    }
}