package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ProductoJpaDataRepository extends JpaRepository<ProductoEntity, UUID> {
}