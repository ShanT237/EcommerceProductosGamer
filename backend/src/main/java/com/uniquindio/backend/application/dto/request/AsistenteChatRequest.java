package com.uniquindio.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// Usado en: POST /api/asistente/comprador y /api/asistente/vendedor
public record AsistenteChatRequest(
        @NotBlank @Size(max = 1000) String mensaje,
        @Size(max = 10) List<@Valid MensajeHistorialRequest> historial
) {}
