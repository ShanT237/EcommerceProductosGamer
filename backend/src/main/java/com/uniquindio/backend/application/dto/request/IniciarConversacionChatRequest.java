package com.uniquindio.backend.application.dto.request;

import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Usado en: POST /api/chat/conversaciones
// vendedorId solo se usa en órdenes con varios vendedores; para PRODUCTO se ignora.
public record IniciarConversacionChatRequest(
        @NotNull TipoContextoChat tipo,
        @NotNull UUID referenciaId,
        UUID vendedorId
) {
}
