package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ResenaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface ResenaJpaDataRepository extends JpaRepository<ResenaEntity, String> {

    boolean existsByIdUsuarioAndIdProducto(String idUsuario, String idProducto);
}