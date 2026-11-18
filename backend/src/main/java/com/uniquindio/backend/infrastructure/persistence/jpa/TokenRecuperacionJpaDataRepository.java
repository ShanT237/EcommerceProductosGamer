package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.TokenRecuperacionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TokenRecuperacionJpaDataRepository extends JpaRepository<TokenRecuperacionEntity, UUID> {

    Optional<TokenRecuperacionEntity> findByHashToken(String hashToken);

    Optional<TokenRecuperacionEntity> findFirstByCuentaIdOrderByCreadoEnDesc(UUID cuentaId);

    void deleteByCuentaId(UUID cuentaId);
}
