package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.valueobject.EstadoRegalo;

import java.util.UUID;

// Usado en: POST /api/regalos/elegir
public record ElegirRegaloResponse(
        UUID id,
        UUID idUsuario,
        UUID idProducto,
        UUID idCombo,
        EstadoRegalo estado
) {}