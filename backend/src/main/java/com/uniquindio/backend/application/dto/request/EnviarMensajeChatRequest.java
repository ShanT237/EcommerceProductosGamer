package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Usado en: POST /api/chat/conversaciones/{id}/mensajes
// El remitente sale siempre del JWT; clienteId lo genera el cliente para que el reenvío sea idempotente.
public record EnviarMensajeChatRequest(
        @NotNull UUID clienteId,
        @NotBlank @Size(max = 2000) String contenido
) {
}
