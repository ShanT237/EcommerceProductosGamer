package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Combo;

import java.util.Optional;
import java.util.UUID;

public interface ComboRepository {
    Optional<Combo> obtenerPorId(UUID id);
    void guardar(Combo combo);
}
