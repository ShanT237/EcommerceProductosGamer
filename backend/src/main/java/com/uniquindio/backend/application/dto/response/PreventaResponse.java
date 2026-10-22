package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Preventa;

import java.time.LocalDate;
import java.util.UUID;

// Usado en: POST /api/preventas
public record PreventaResponse(
        UUID id,
        UUID productoId,
        LocalDate fechaLanzamiento,
        String descripcion
) {
    public static PreventaResponse desde(Preventa preventa) {
        return new PreventaResponse(
                preventa.getId(),
                preventa.getProductoId(),
                preventa.getFechaLanzamiento(),
                preventa.getDescripcion()
        );
    }
}