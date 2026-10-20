package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CompraEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

interface CompraJpaDataRepository extends JpaRepository<CompraEntity, UUID> {

    List<CompraEntity> findByUsuarioId(UUID usuarioId);

    boolean existsByUsuarioIdAndProductoIdAndEstadoIn(
            UUID usuarioId, UUID productoId, Collection<EstadoCompra> estados);

    boolean existsByProductoIdAndEstadoIn(UUID productoId, Collection<EstadoCompra> estados);


    long countByUsuarioIdAndEstado(UUID usuarioId, EstadoCompra estado);
}