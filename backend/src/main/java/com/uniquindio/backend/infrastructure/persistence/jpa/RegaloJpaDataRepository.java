package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.RegaloEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface RegaloJpaDataRepository extends JpaRepository<RegaloEntity, UUID> {

    long countByIdUsuarioAndEstadoNot(UUID idUsuario, EstadoRegalo estado);
}