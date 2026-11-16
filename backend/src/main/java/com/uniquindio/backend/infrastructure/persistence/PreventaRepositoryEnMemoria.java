package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;

import java.util.HashMap;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PreventaRepositoryEnMemoria implements PreventaRepository {

    private final Map<UUID, Preventa> preventas = new HashMap<>();

    @Override
    public Optional<Preventa> obtenerPorId(UUID id) {
        return Optional.ofNullable(preventas.get(id));
    }

    @Override
    public Optional<Preventa> obtenerPorProductoId(UUID productoId) {
        return preventas.values().stream()
                .filter(p -> p.getProductoId().equals(productoId))
                .findFirst();
    }

    @Override
    public List<Preventa> obtenerFuturas(LocalDate hoy) {
        return preventas.values().stream()
                .filter(preventa -> preventa.getFechaLanzamiento().isAfter(hoy))
                .toList();
    }

    @Override
    public List<Preventa> obtenerLanzadasEn(LocalDate fecha) {
        return preventas.values().stream()
                .filter(preventa -> preventa.getFechaLanzamiento().isEqual(fecha))
                .toList();
    }

    @Override
    public void guardar(Preventa preventa) {
        preventas.put(preventa.getId(), preventa);
    }

    @Override
    public void eliminar(UUID id) {
        preventas.remove(id);
    }
}
