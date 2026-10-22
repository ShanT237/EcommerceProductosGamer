package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ValoracionVendedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface ValoracionVendedorJpaDataRepository extends JpaRepository<ValoracionVendedorEntity, UUID> {

    boolean existsByCompraId(UUID compraId);

    List<ValoracionVendedorEntity> findByVendedorId(UUID vendedorId);
}