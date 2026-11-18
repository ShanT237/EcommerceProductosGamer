package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ConversacionChatResponse;
import com.uniquindio.backend.application.dto.response.HistorialChatResponse;
import com.uniquindio.backend.application.dto.response.MarcadosLeidosResponse;
import com.uniquindio.backend.application.dto.response.MensajeChatResponse;
import com.uniquindio.backend.application.usecase.HistorialChat;
import com.uniquindio.backend.application.usecase.ResumenConversacionChat;
import com.uniquindio.backend.domain.entity.MensajeChat;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMapper {

    public ConversacionChatResponse toResponse(ResumenConversacionChat resumen) {
        return ConversacionChatResponse.desde(resumen);
    }

    public List<ConversacionChatResponse> toResponses(List<ResumenConversacionChat> resumenes) {
        return resumenes.stream().map(this::toResponse).toList();
    }

    public MensajeChatResponse toResponse(MensajeChat mensaje) {
        return MensajeChatResponse.desde(mensaje);
    }

    public HistorialChatResponse toResponse(HistorialChat historial) {
        return HistorialChatResponse.desde(historial);
    }

    public MarcadosLeidosResponse toMarcados(int marcados) {
        return new MarcadosLeidosResponse(marcados);
    }
}
