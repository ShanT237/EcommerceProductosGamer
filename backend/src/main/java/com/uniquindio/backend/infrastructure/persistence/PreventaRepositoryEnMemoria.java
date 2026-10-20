package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;


public class PreventaRepositoryEnMemoria implements PreventaRepository {

    private final Map<UUID, Preventa> preventas = new HashMap<>();

    @Override
    public Optional<Preventa> obtenerPorProductoId(UUID productoId) {
        return preventas.values().stream()
                .filter(p -> p.getProductoId().equals(productoId))
                .findFirst();
    }

    @Override
    public void guardar(Preventa preventa) {
        preventas.put(preventa.getId(), preventa);
    }
}