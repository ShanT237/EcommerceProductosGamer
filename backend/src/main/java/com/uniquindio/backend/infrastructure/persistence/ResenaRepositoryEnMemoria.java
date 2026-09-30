package com.uniquindio.backend.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.repository.ResenaRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class ResenaRepositoryEnMemoria implements ResenaRepository {

    private final Map<String, Resena> resenas = new HashMap<>();

    @Override
    public Optional<Resena> obtenerPorId(String id) {
        return Optional.ofNullable(resenas.get(id));
    }

    @Override
    public void guardar(Resena resena) {
        resenas.put(resena.getId(), resena);
    }
}