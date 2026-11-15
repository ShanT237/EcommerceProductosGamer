package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

public class CuponRepositoryEnMemoria implements CuponRepository {

    private final Map<String, Cupon> cupones = new HashMap<>();
    private final Set<String> usosPorComprador = new HashSet<>();   // "cuponId:usuarioId"

    @Override
    public void guardar(Cupon cupon) {
        cupones.put(cupon.getCodigo(), cupon);
    }

    @Override
    public Optional<Cupon> obtenerPorCodigo(String codigo) {
        return codigo == null ? Optional.empty()
                : Optional.ofNullable(cupones.get(codigo.trim().toUpperCase()));
    }

    @Override
    public List<Cupon> obtenerPorVendedorId(UUID vendedorId) {
        return cupones.values().stream()
                .filter(c -> vendedorId != null && vendedorId.equals(c.getVendedorId()))
                .toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return obtenerPorCodigo(codigo).isPresent();
    }

    @Override
    public boolean fueUsadoPor(UUID cuponId, UUID usuarioId) {
        return usosPorComprador.contains(cuponId + ":" + usuarioId);
    }

    @Override
    public void registrarUsoDe(UUID cuponId, UUID usuarioId) {
        usosPorComprador.add(cuponId + ":" + usuarioId);
    }
}
