package com.uniquindio.backend.application.dto;

import java.time.LocalDate;
import java.util.UUID;

public record RealizarCompraRequestDTO(
        UUID id,
        UUID usuarioId,
        UUID productoId,
        int cantidad,
        LocalDate fecha
) {
}