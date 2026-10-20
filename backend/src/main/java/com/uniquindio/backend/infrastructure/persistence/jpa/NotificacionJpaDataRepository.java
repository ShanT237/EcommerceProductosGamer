package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.NotificacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface NotificacionJpaDataRepository extends JpaRepository<NotificacionEntity, UUID> {

    List<NotificacionEntity> findByUsuarioId(UUID usuarioId);
}