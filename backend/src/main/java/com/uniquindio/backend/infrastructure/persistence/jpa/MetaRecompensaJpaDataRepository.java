package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MetaRecompensaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MetaRecompensaJpaDataRepository extends JpaRepository<MetaRecompensaEntity, UUID> {

    List<MetaRecompensaEntity> findByActivaTrue();
}