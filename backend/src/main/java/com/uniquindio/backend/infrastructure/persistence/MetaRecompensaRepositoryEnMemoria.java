package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.repository.MetaRecompensaRepository;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MetaRecompensaRepositoryEnMemoria implements MetaRecompensaRepository {

    private final Map<UUID, MetaRecompensa> metas = new HashMap<>();

    @Override
    public Optional<MetaRecompensa> obtenerPorId(UUID id) {
        return Optional.ofNullable(metas.get(id));
    }

    @Override
    public void guardar(MetaRecompensa meta) {
        metas.put(meta.getId(), meta);
    }

    @Override
    public List<MetaRecompensa> obtenerActivas() {
        return metas.values().stream().filter(MetaRecompensa::isActiva).toList();
    }
}
