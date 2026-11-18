package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.MensajeChat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MensajeChatRepository {
    void guardar(MensajeChat mensaje);
    Optional<MensajeChat> obtenerPorId(UUID id);
    Optional<MensajeChat> obtenerPorRemitenteYClienteId(UUID remitenteId, UUID clienteId);

    /** Mensajes anteriores al cursor (nulo = desde el más nuevo), el más nuevo primero. */
    List<MensajeChat> obtenerAnteriores(UUID conversacionId, MensajeChat cursor, int limite);

    /** Mensajes posteriores al cursor, el más antiguo primero. */
    List<MensajeChat> obtenerPosteriores(UUID conversacionId, MensajeChat cursor, int limite);

    /** Mensajes no leídos que el usuario ha recibido, agrupados por conversación. */
    Map<UUID, Long> contarNoLeidosPorConversacion(UUID destinatarioId);

    /** Marca como leídos los mensajes ajenos pendientes; devuelve cuántos cambió. */
    int marcarLeidos(UUID conversacionId, UUID lectorId, LocalDateTime cuando);
}
