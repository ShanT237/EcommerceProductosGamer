package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.ArmadorResumenChat;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Conversaciones del usuario autenticado (la más reciente primero), con sus mensajes sin leer. */
@Service
@RequiredArgsConstructor
public class ListarConversacionesChatUseCase {

    private final ConversacionChatRepository conversacionRepository;
    private final MensajeChatRepository mensajeRepository;
    private final ArmadorResumenChat armador;

    @Transactional(readOnly = true)
    public List<ResumenConversacionChat> ejecutar(UUID usuarioId) {
        Map<UUID, Long> noLeidos = mensajeRepository.contarNoLeidosPorConversacion(usuarioId);
        return conversacionRepository.obtenerPorParticipante(usuarioId).stream()
                .map(c -> armador.armar(c, usuarioId, noLeidos.getOrDefault(c.getId(), 0L)))
                .toList();
    }
}
