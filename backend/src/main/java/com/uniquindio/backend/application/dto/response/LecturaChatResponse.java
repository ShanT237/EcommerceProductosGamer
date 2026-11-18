package com.uniquindio.backend.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: evento SSE "leidos"
public record LecturaChatResponse(
        UUID conversacionId,
        UUID lectorId,
        LocalDateTime leidoEn
) {
}
