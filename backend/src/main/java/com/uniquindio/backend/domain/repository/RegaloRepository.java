package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Regalo;

import java.util.Optional;
import java.util.UUID;

public interface RegaloRepository {
    Optional<Regalo> obtenerPorId(UUID id);
    void guardar(Regalo regalo);
}
