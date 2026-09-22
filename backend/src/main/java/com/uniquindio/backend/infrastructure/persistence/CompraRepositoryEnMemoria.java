package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.repository.CompraRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CompraRepositoryEnMemoria implements CompraRepository {

    private final Map<UUID, Compra> compras = new HashMap<>();

    @Override
    public Optional<Compra> obtenerPorId(UUID id) {
        return Optional.ofNullable(compras.get(id));
    }

    @Override
    public void guardar(Compra compra) {
        compras.put(compra.getId(), compra);
    }

    @Override
    public boolean existeCompraDe(UUID usuarioId, UUID productoId) {
        return compras.values().stream()
                .anyMatch(compra -> compra.getUsuarioId().equals(usuarioId)
                        && compra.getProductoId().equals(productoId));
    }
}