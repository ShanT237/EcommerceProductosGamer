package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.RegaloEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface RegaloJpaDataRepository extends JpaRepository<RegaloEntity, UUID> {

    List<RegaloEntity> findByEstado(EstadoRegalo estado);

    List<RegaloEntity> findByIdUsuario(UUID idUsuario);

    long countByIdUsuarioAndEstadoNot(UUID idUsuario, EstadoRegalo estado);

    boolean existsByIdComboAndEstadoNotIn(UUID idCombo, Collection<EstadoRegalo> estados);
}
