package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PromocionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface PromocionJpaDataRepository extends JpaRepository<PromocionEntity, UUID> {

    List<PromocionEntity> findByProductoId(UUID productoId);

    // Vigente = desde <= fecha <= hasta (igual que Promocion.estaVigente)
    @Query("select p from PromocionEntity p "
            + "where p.productoId = :productoId and p.desde <= :fecha and p.hasta >= :fecha")
    List<PromocionEntity> findVigentes(@Param("productoId") UUID productoId,
                                       @Param("fecha") LocalDate fecha);
}