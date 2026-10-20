package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.UsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UsuarioJpaDataRepository extends JpaRepository<UsuarioEntity, UUID> {

    boolean existsByCorreo(String correo);
}