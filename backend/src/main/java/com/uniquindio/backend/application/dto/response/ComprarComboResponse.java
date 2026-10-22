package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.List;
import java.util.UUID;

// Usado en: POST /api/combos/{id}/comprar
// Una compra PENDIENTE por cada producto del combo, con el descuento del combo ya aplicado.
public record ComprarComboResponse(
        UUID comboId,
        List<RealizarCompraResponse> compras,
        double total
) {
    public static ComprarComboResponse desde(UUID comboId, List<Compra> compras) {
        List<RealizarCompraResponse> detalle = compras.stream()
                .map(RealizarCompraResponse::desde)
                .toList();
        double total = compras.stream()
                .mapToDouble(c -> c.getSubtotal().valor())
                .sum();
        return new ComprarComboResponse(comboId, detalle, total);
    }
}