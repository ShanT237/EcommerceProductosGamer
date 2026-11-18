package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.valueobject.TipoContextoChat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversacionChatRepository {
    void guardar(ConversacionChat conversacion);
    Optional<ConversacionChat> obtenerPorId(UUID id);
    Optional<ConversacionChat> obtenerPorContexto(UUID compradorId, UUID vendedorId,
                                                  TipoContextoChat tipo, UUID referenciaId);

    /** Conversaciones donde participa el usuario, la de actividad más reciente primero. */
    List<ConversacionChat> obtenerPorParticipante(UUID usuarioId);
}
