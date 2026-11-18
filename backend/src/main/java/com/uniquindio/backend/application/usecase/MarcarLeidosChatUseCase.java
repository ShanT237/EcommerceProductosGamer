package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.port.out.CanalChatEnVivo;
import com.uniquindio.backend.application.port.out.EventoChat;
import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Marca como leídos los mensajes que el otro participante envió y avisa a ambos en vivo. */
@Service
@RequiredArgsConstructor
public class MarcarLeidosChatUseCase {

    private final ConversacionChatRepository conversacionRepository;
    private final MensajeChatRepository mensajeRepository;
    private final CanalChatEnVivo canal;
    private final Clock clock;

    @Transactional
    public int ejecutar(UUID lectorId, UUID conversacionId) {
        ConversacionChat conversacion = conversacionRepository.obtenerPorId(conversacionId)
                .orElseThrow(() -> new NoSuchElementException("La conversación no existe"));
        conversacion.asegurarParticipante(lectorId);

        LocalDateTime ahora = LocalDateTime.now(clock);
        int marcados = mensajeRepository.marcarLeidos(conversacionId, lectorId, ahora);
        if (marcados > 0) {
            // El otro participante ve "Visto"; las demás pestañas del lector limpian su contador.
            canal.publicar(List.of(conversacion.getCompradorId(), conversacion.getVendedorId()),
                    new EventoChat.MensajesLeidos(conversacionId, lectorId, ahora));
        }
        return marcados;
    }
}
