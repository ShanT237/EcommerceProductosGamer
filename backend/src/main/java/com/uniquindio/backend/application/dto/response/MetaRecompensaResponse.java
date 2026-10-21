package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.MetaRecompensa;

import java.util.UUID;

// Usado en: /api/metas-recompensa
public record MetaRecompensaResponse(
        UUID id,
        int comprasRequeridas,
        UUID productoRegaloId,
        boolean activa
) {
    public static MetaRecompensaResponse desde(MetaRecompensa meta) {
        return new MetaRecompensaResponse(
                meta.getId(),
                meta.getComprasRequeridas(),
                meta.getProductoRegaloId(),
                meta.isActiva()
        );
    }
}