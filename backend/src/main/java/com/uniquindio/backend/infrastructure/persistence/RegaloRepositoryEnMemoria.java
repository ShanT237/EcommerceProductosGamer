package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RegaloRepositoryEnMemoria implements RegaloRepository {

    private final Map<UUID, Regalo> regalos = new HashMap<>();

    @Override
    public Optional<Regalo> obtenerPorId(UUID id) {
        return Optional.ofNullable(regalos.get(id));
    }

    @Override
    public void guardar(Regalo regalo) {
        regalos.put(regalo.getId(), regalo);
    }
}