package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: POST /api/valoraciones-vendedor
public record ValoracionVendedorResponse(
        UUID id,
        UUID compraId,
        UUID usuarioId,
        UUID vendedorId,
        int estrellas,
        String comentario,
        LocalDateTime fecha
) {
    public static ValoracionVendedorResponse desde(ValoracionVendedor valoracion) {
        return new ValoracionVendedorResponse(
                valoracion.getId(),
                valoracion.getCompraId(),
                valoracion.getUsuarioId(),
                valoracion.getVendedorId(),
                valoracion.getCalificacion().estrellas(),
                valoracion.getComentario(),
                valoracion.getFecha()
        );
    }
}