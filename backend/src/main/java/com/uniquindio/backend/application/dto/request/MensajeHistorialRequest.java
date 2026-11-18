package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Usado en: POST /api/asistente/comprador y /api/asistente/vendedor (turnos anteriores de la conversación)
public record MensajeHistorialRequest(
        @NotNull @Pattern(regexp = "user|assistant") String rol,
        @NotBlank @Size(max = 2000) String contenido
) {}
