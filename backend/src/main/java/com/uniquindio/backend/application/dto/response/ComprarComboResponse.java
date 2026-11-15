package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.List;
import java.util.UUID;

// Usado en: POST /api/combos/{id}/comprar
// Una compra PENDIENTE por cada producto del combo, con el descuento del combo ya aplicado.
public record ComprarComboResponse(
        UUID comboId,
        UUID pedidoId,
        List<RealizarCompraResponse> compras,
        double total,
        boolean cuponAplicado
) {
    public static ComprarComboResponse desde(UUID comboId, List<Compra> compras) {
        List<RealizarCompraResponse> detalle = compras.stream()
                .map(RealizarCompraResponse::desde)
                .toList();
        double total = compras.stream()
                .mapToDouble(c -> c.getSubtotal().valor())
                .sum();
        UUID pedidoId = compras.isEmpty() ? null : compras.get(0).getPedidoId();
        boolean cuponAplicado = compras.stream().anyMatch(Compra::isCuponAplicado);
        return new ComprarComboResponse(comboId, pedidoId, detalle, total, cuponAplicado);
    }
}