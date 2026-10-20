package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Preventa;

import java.util.Optional;
import java.util.UUID;

public interface PreventaRepository {

    /** Usado por CU-37 para rechazar una segunda preventa del mismo producto. */
    Optional<Preventa> obtenerPorProductoId(UUID productoId);

    void guardar(Preventa preventa);
}