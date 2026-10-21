package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;

import java.util.UUID;

// Usado en: PUT /api/compras/{id}/cupon
public record AplicarCuponResponse(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        double precioUnitario,
        double subtotal,
        boolean cuponAplicado,
        EstadoCompra estado
) {
    public static AplicarCuponResponse desde(Compra compra) {
        return new AplicarCuponResponse(
                compra.getId(),
                compra.getUsuarioId(),
                compra.getProductoId(),
                compra.getPrecioUnitario().valor(),
                compra.getSubtotal().valor(),
                compra.isCuponAplicado(),
                compra.getEstado()
        );
    }
}