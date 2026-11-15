package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CuponEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CuponJpaDataRepository extends JpaRepository<CuponEntity, UUID> {

    Optional<CuponEntity> findByCodigo(String codigo);

    List<CuponEntity> findByVendedorId(UUID vendedorId);

    boolean existsByCodigo(String codigo);
}
