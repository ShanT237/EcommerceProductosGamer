package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.application.usecase.HistorialChat;

import java.util.List;

// Usado en: GET /api/chat/conversaciones/{id}/mensajes
public record HistorialChatResponse(List<MensajeChatResponse> mensajes, boolean hayMas) {

    public static HistorialChatResponse desde(HistorialChat historial) {
        return new HistorialChatResponse(
                historial.mensajes().stream().map(MensajeChatResponse::desde).toList(),
                historial.hayMas());
    }
}
