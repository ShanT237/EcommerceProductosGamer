package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ConversacionChatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface ConversacionChatJpaDataRepository extends JpaRepository<ConversacionChatEntity, UUID> {

    Optional<ConversacionChatEntity> findByCompradorIdAndVendedorIdAndTipoAndReferenciaId(
            UUID compradorId, UUID vendedorId, TipoContextoChat tipo, UUID referenciaId);

    List<ConversacionChatEntity> findByCompradorIdOrVendedorIdOrderByUltimaActividadDesc(
            UUID compradorId, UUID vendedorId);
}
