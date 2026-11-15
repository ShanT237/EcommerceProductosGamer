package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CuponUsoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface CuponUsoJpaDataRepository extends JpaRepository<CuponUsoEntity, UUID> {

    boolean existsByCuponIdAndUsuarioId(UUID cuponId, UUID usuarioId);
}
