package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;

import java.time.LocalDate;
import java.util.UUID;

public record RealizarCompraResponseDTO(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        int cantidad,
        double precioUnitario,
        double subtotal,
        LocalDate fecha,
        EstadoCompra estado
) {
    public static RealizarCompraResponseDTO desde(Compra compra) {
        return new RealizarCompraResponseDTO(
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