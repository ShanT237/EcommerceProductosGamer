package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.port.out.CanalChatEnVivo;
import com.uniquindio.backend.application.port.out.EventoChat;
import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.entity.MensajeChat;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Envía un mensaje a la conversación. Es idempotente por (remitente, clienteId): un reintento tras
 * una caída de red devuelve el mensaje ya guardado sin duplicarlo ni republicarlo.
 */
@Service
@RequiredArgsConstructor
public class EnviarMensajeChatUseCase {

    private final ConversacionChatRepository conversacionRepository;
    private final MensajeChatRepository mensajeRepository;
    private final CanalChatEnVivo canal;
    private final Clock clock;

    @Transactional
    public MensajeChat ejecutar(UUID remitenteId, UUID conversacionId, UUID clienteId, String contenido) {
        ConversacionChat conversacion = conversacionRepository.obtenerPorId(conversacionId)
                .orElseThrow(() -> new NoSuchElementException("La conversación no existe"));
        conversacion.asegurarParticipante(remitenteId);

        Optional<MensajeChat> previo = mensajeRepository.obtenerPorRemitenteYClienteId(remitenteId, clienteId);
        if (previo.isPresent()) {
            if (!previo.get().getConversacionId().equals(conversacionId)) {
                throw new ReglaDominioException("El clienteId ya se usó en otra conversación");
            }
            return previo.get();
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        MensajeChat mensaje = MensajeChat.enviar(UUID.randomUUID(), conversacionId, remitenteId,
                clienteId, contenido, ahora);
        mensajeRepository.guardar(mensaje);
        conversacion.registrarActividad(mensaje.getContenido(), ahora);
        conversacionRepository.guardar(conversacion);

        // Incluye al remitente para sincronizar sus otras pestañas; el canal difiere el envío a afterCommit.
        canal.publicar(List.of(conversacion.getCompradorId(), conversacion.getVendedorId()),
                new EventoChat.MensajeNuevo(mensaje));
        return mensaje;
    }
}
