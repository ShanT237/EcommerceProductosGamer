package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.MensajeChat;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: eventos SSE "mensaje", historial y envío de mensajes del chat
public record MensajeChatResponse(
        UUID id,
        UUID conversacionId,
        UUID remitenteId,
        UUID clienteId,
        String contenido,
        LocalDateTime enviadoEn,
        LocalDateTime leidoEn
) {
    public static MensajeChatResponse desde(MensajeChat m) {
        return new MensajeChatResponse(m.getId(), m.getConversacionId(), m.getRemitenteId(),
                m.getClienteId(), m.getContenido(), m.getEnviadoEn(), m.getLeidoEn());
    }
}
