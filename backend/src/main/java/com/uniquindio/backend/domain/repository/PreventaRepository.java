package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Preventa;

import java.util.Optional;
import java.util.UUID;

public interface PreventaRepository {
    Optional<Preventa> obtenerPorProductoId(UUID productoId);
    void guardar(Preventa preventa);
}