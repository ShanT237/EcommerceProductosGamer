package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MovimientoPuntosEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MovimientoPuntosJpaDataRepository extends JpaRepository<MovimientoPuntosEntity, UUID> {

    // Orden cronológico, igual que la lista del repo en memoria
    List<MovimientoPuntosEntity> findByUsuarioIdOrderByFechaAsc(UUID usuarioId);
}