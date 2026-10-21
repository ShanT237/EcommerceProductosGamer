package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ComboEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ComboJpaDataRepository extends JpaRepository<ComboEntity, UUID> {
}