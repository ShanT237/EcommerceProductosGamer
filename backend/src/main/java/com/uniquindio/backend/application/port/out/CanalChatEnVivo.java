package com.uniquindio.backend.application.port.out;

import java.util.Collection;
import java.util.UUID;

/** Puerto de salida: publica eventos del chat, únicamente, a los usuarios indicados. */
public interface CanalChatEnVivo {
    void publicar(Collection<UUID> destinatarios, EventoChat evento);
}
