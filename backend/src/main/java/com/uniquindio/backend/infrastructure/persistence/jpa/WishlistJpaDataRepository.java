package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.WishlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface WishlistJpaDataRepository extends JpaRepository<WishlistEntity, UUID> {

    Optional<WishlistEntity> findByUsuarioId(UUID usuarioId);

    @Query("SELECT w FROM WishlistEntity w WHERE :productoId MEMBER OF w.productos")
    List<WishlistEntity> findByProductoIdEnProductos(@Param("productoId") UUID productoId);
}