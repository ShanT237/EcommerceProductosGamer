package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.MensajeChat;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.MensajeChatPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MensajeChatRepositoryJpa implements MensajeChatRepository {

    private final MensajeChatJpaDataRepository dataRepository;
    private final MensajeChatPersistenceMapper mapper;

    @Override
    public void guardar(MensajeChat mensaje) {
        dataRepository.save(mapper.toEntity(mensaje));
    }

    @Override
    public Optional<MensajeChat> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<MensajeChat> obtenerPorRemitenteYClienteId(UUID remitenteId, UUID clienteId) {
        return dataRepository.findByRemitenteIdAndClienteId(remitenteId, clienteId).map(mapper::toDomain);
    }

    @Override
    public List<MensajeChat> obtenerAnteriores(UUID conversacionId, MensajeChat cursor, int limite) {
        Pageable pagina = PageRequest.of(0, limite);
        var entidades = cursor == null
                ? dataRepository.findByConversacionIdOrderByEnviadoEnDescIdDesc(conversacionId, pagina)
                : dataRepository.anterioresA(conversacionId, cursor.getEnviadoEn(), cursor.getId(), pagina);
        return entidades.stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<MensajeChat> obtenerPosteriores(UUID conversacionId, MensajeChat cursor, int limite) {
        return dataRepository
                .posterioresA(conversacionId, cursor.getEnviadoEn(), cursor.getId(), PageRequest.of(0, limite))
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Map<UUID, Long> contarNoLeidosPorConversacion(UUID destinatarioId) {
        Map<UUID, Long> resultado = new HashMap<>();
        for (Object[] fila : dataRepository.contarNoLeidos(destinatarioId)) {
            resultado.put((UUID) fila[0], ((Number) fila[1]).longValue());
        }
        return resultado;
    }

    @Override
    public int marcarLeidos(UUID conversacionId, UUID lectorId, LocalDateTime cuando) {
        return dataRepository.marcarLeidos(conversacionId, lectorId, cuando);
    }
}
