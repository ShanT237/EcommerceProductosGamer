package com.uniquindio.backend.application.port.out;

import com.uniquindio.backend.domain.entity.MensajeChat;

import java.time.LocalDateTime;
import java.util.UUID;

/** Novedades del chat que se empujan en vivo a los participantes de una conversación. */
public sealed interface EventoChat {

    /** Se envió un mensaje nuevo. */
    record MensajeNuevo(MensajeChat mensaje) implements EventoChat {
    }

    /** {@code lectorId} leyó los mensajes pendientes de la conversación. */
    record MensajesLeidos(UUID conversacionId, UUID lectorId, LocalDateTime leidoEn) implements EventoChat {
    }
}
