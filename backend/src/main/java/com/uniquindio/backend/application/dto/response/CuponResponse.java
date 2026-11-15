package com.uniquindio.backend.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

// Usado en: POST /api/cupones, GET /api/cupones y GET /api/cupones/{codigo}
// vendedorId es null cuando el cupón es global (aplica a cualquier producto).
public record CuponResponse(
        UUID id,
        String codigo,
        double porcentaje,
        LocalDate vence,
        int usosMaximos,
        int usos,
        UUID vendedorId
) {}
