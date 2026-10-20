package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.NotificacionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificacionRepositoryJpa implements NotificacionRepository {

    private final NotificacionJpaDataRepository dataRepository;
    private final NotificacionPersistenceMapper mapper;

    @Override
    public void guardar(Notificacion notificacion) {
        dataRepository.save(mapper.toEntity(notificacion));
    }

    @Override
    public Optional<Notificacion> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Notificacion> obtenerPorUsuarioId(UUID usuarioId) {
        return dataRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}