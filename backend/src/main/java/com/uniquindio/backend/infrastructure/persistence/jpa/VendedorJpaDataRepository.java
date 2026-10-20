package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.VendedorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface VendedorJpaDataRepository extends JpaRepository<VendedorEntity, UUID> {

    boolean existsByDocumento(String documento);
}