package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;

import java.util.UUID;

public record ConfirmarCompraResponse(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        double subtotal,
        EstadoCompra estado
) {
    public static ConfirmarCompraResponse desde(Compra compra) {
        return new ConfirmarCompraResponse(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getProductoId(),
                compra.getSubtotal().valor(),
                compra.getEstado()
        );
    }
}