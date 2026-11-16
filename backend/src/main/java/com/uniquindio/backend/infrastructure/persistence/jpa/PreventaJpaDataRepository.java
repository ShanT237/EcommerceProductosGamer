package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PreventaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

interface PreventaJpaDataRepository extends JpaRepository<PreventaEntity, UUID> {

    Optional<PreventaEntity> findByProductoId(UUID productoId);

    List<PreventaEntity> findByFechaLanzamientoAfter(LocalDate hoy);

    List<PreventaEntity> findByFechaLanzamiento(LocalDate fecha);
}
