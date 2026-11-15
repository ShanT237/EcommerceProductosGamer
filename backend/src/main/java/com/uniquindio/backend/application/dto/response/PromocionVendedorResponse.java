package com.uniquindio.backend.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

// Usado en: GET /api/promociones (panel del vendedor)
public record PromocionVendedorResponse(
        UUID id,
        UUID productoId,
        String productoNombre,
        double porcentaje,
        LocalDate desde,
        LocalDate hasta
) {}