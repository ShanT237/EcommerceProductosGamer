package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Promocion;

import java.time.LocalDate;
import java.util.UUID;

// Usado en: POST /api/promociones
public record PromocionResponse(
        UUID id,
        UUID productoId,
        double porcentaje,
        LocalDate desde,
        LocalDate hasta
) {
    public static PromocionResponse desde(Promocion promocion) {
        return new PromocionResponse(
                promocion.getId(),
                promocion.getProductoId(),
                promocion.getPorcentaje(),
                promocion.getDesde(),
                promocion.getHasta()
        );
    }
}