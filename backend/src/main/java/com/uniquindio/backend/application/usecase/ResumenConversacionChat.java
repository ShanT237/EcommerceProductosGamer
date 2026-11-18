package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.ConversacionChat;

/** Conversación vista por un usuario: con título, nombre de la contraparte y mensajes sin leer. */
public record ResumenConversacionChat(
        ConversacionChat conversacion,
        String titulo,
        String contraparteNombre,
        long noLeidos
) {
}
