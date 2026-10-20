package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.MetaRecompensa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MetaRecompensaRepository {
    Optional<MetaRecompensa> obtenerPorId(UUID id);
    void guardar(MetaRecompensa meta);
    List<MetaRecompensa> obtenerActivas();
}
