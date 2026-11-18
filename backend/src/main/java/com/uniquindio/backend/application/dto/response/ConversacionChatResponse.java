package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.application.usecase.ResumenConversacionChat;
import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.valueobject.TipoContextoChat;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: POST y GET /api/chat/conversaciones
public record ConversacionChatResponse(
        UUID id,
        TipoContextoChat tipo,
        UUID referenciaId,
        UUID compradorId,
        UUID vendedorId,
        String titulo,
        String contraparteNombre,
        String vistaPrevia,
        LocalDateTime ultimaActividad,
        long noLeidos
) {
    public static ConversacionChatResponse desde(ResumenConversacionChat resumen) {
        ConversacionChat c = resumen.conversacion();
        return new ConversacionChatResponse(c.getId(), c.getTipo(), c.getReferenciaId(),
                c.getCompradorId(), c.getVendedorId(), resumen.titulo(), resumen.contraparteNombre(),
                c.getVistaPrevia(), c.getUltimaActividad(), resumen.noLeidos());
    }
}
