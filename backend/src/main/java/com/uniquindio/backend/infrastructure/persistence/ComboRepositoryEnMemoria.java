package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.repository.ComboRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ComboRepositoryEnMemoria implements ComboRepository {

    private final Map<UUID, Combo> combos = new LinkedHashMap<>();

    @Override
    public Optional<Combo> obtenerPorId(UUID id) {
        return Optional.ofNullable(combos.get(id));
    }

    @Override
    public void guardar(Combo combo) {
        combos.put(combo.getId(), combo);
    }

    @Override
    public List<Combo> obtenerTodos() {
        return List.copyOf(combos.values());
    }

    @Override
    public List<Combo> obtenerPorVendedorId(UUID vendedorId) {
        return combos.values().stream()
                .filter(c -> c.getVendedorId().equals(vendedorId))
                .toList();
    }
}
