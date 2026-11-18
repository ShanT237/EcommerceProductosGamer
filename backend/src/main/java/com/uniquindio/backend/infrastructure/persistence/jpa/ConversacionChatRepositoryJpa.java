package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ConversacionChatPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConversacionChatRepositoryJpa implements ConversacionChatRepository {

    private final ConversacionChatJpaDataRepository dataRepository;
    private final ConversacionChatPersistenceMapper mapper;

    @Override
    public void guardar(ConversacionChat conversacion) {
        dataRepository.save(mapper.toEntity(conversacion));
    }

    @Override
    public Optional<ConversacionChat> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<ConversacionChat> obtenerPorContexto(UUID compradorId, UUID vendedorId,
                                                         TipoContextoChat tipo, UUID referenciaId) {
        return dataRepository
                .findByCompradorIdAndVendedorIdAndTipoAndReferenciaId(compradorId, vendedorId, tipo, referenciaId)
                .map(mapper::toDomain);
    }

    @Override
    public List<ConversacionChat> obtenerPorParticipante(UUID usuarioId) {
        return dataRepository.findByCompradorIdOrVendedorIdOrderByUltimaActividadDesc(usuarioId, usuarioId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
