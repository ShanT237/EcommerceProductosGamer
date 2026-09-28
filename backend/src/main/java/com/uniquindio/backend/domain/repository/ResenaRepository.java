package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Resena;

import java.util.Optional;

public interface ResenaRepository {
    Optional<Resena> obtenerPorId(String id);
    void guardar(Resena resena);
}
