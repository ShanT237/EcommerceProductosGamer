package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.List;
import java.util.UUID;

// Usado en: POST /api/pedidos y sus acciones de confirmar, cancelar o reembolsar
// Una compra por cada línea del pedido; el total es la suma de los subtotales
// (ya con el descuento del cupón en las líneas a las que aplicó).
public record PedidoResponse(
        UUID pedidoId,
        List<RealizarCompraResponse> compras,
        double total,
        boolean cuponAplicado   // true si alguna línea del pedido recibió el descuento de un cupón
) {
    /** Las compras deben pertenecer al mismo pedido y no estar vacías (lo garantizan los casos de uso). */
    public static PedidoResponse desde(List<Compra> compras) {
        List<RealizarCompraResponse> detalle = compras.stream()
                .map(RealizarCompraResponse::desde)
                .toList();
        double total = compras.stream()
                .mapToDouble(c -> c.getSubtotal().valor())
                .sum();
        boolean cuponAplicado = compras.stream().anyMatch(Compra::isCuponAplicado);
        return new PedidoResponse(compras.get(0).getPedidoId(), detalle, total, cuponAplicado);
    }
}
