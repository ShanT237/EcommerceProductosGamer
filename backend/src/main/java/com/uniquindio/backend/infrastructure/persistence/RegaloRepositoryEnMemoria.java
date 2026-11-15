package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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

    @Override
    public List<Regalo> obtenerPorEstado(EstadoRegalo estado) {
        return regalos.values().stream().filter(r -> r.getEstado() == estado).toList();
    }

    @Override
    public List<Regalo> obtenerPorUsuarioId(UUID usuarioId) {
        return regalos.values().stream().filter(r -> usuarioId.equals(r.getIdUsuario())).toList();
    }

    @Override
    public long contarAsignadosA(UUID usuarioId) {
        return regalos.values().stream()
                .filter(r -> usuarioId.equals(r.getIdUsuario())
                        && r.getEstado() != EstadoRegalo.CANCELADO)
                .count();
    }

    @Override
    public boolean existePendienteDelCombo(UUID comboId) {
        return regalos.values().stream()
                .anyMatch(r -> comboId.equals(r.getIdCombo()) && !r.getEstado().esFinal());
    }
}
