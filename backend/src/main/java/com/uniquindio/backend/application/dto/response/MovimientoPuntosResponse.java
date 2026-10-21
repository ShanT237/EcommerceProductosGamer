package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: GET /api/puntos/historial
public record MovimientoPuntosResponse(
        UUID id,
        int cantidad,
        TipoMovimientoPuntos tipo,
        LocalDateTime fecha
) {
    public static MovimientoPuntosResponse desde(MovimientoPuntos movimiento) {
        return new MovimientoPuntosResponse(
                movimiento.getId(),
                movimiento.getCantidad(),
                movimiento.getTipo(),
                movimiento.getFecha()
        );
    }
}