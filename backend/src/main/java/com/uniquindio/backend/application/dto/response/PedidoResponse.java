package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.List;
import java.util.UUID;

// Usado en: POST /api/pedidos, PUT /api/pedidos/{id}/confirmar y PUT /api/pedidos/{id}/cancelar
// Una compra por cada línea del pedido; el total es la suma de los subtotales.
public record PedidoResponse(
        UUID pedidoId,
        List<RealizarCompraResponse> compras,
        double total
) {
    /** Las compras deben pertenecer al mismo pedido y no estar vacías (lo garantizan los casos de uso). */
    public static PedidoResponse desde(List<Compra> compras) {
        List<RealizarCompraResponse> detalle = compras.stream()
                .map(RealizarCompraResponse::desde)
                .toList();
        double total = compras.stream()
                .mapToDouble(c -> c.getSubtotal().valor())
                .sum();
        return new PedidoResponse(compras.get(0).getPedidoId(), detalle, total);
    }
}
