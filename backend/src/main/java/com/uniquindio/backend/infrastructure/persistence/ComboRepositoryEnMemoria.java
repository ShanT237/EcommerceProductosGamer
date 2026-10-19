package com.uniquindio.backend.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.repository.ComboRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ComboRepositoryEnMemoria implements ComboRepository {

    private final Map<UUID, Combo> combos = new HashMap<>();

    @Override
    public Optional<Combo> obtenerPorId(UUID id) {
        return Optional.ofNullable(combos.get(id));
    }

    @Override
    public void guardar(Combo combo) {
        combos.put(combo.getId(), combo);
    }
}
