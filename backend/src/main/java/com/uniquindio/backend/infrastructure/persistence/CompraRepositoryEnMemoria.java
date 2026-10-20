package com.uniquindio.backend.infrastructure.persistence;


import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;

import java.util.*;


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
    public List<Compra> obtenerPorUsuarioId(UUID usuarioId) {
        return compras.values().stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId))
                .toList();
    }

    @Override
    public boolean existeCompraDe(UUID usuarioId, UUID productoId) {
        return compras.values().stream()
                .anyMatch(c -> c.getUsuarioId().equals(usuarioId)
                        && c.getProductoId().equals(productoId)
                        && esActiva(c));
    }

    @Override
    public boolean existeCompraActivaDeProducto(UUID productoId) {
        return compras.values().stream()
                .anyMatch(c -> c.getProductoId().equals(productoId) && esActiva(c));
    }

    @Override
    public long contarCompletadasDe(UUID usuarioId) {
        return compras.values().stream()
                .filter(c -> c.getUsuarioId().equals(usuarioId)
                        && c.getEstado() == EstadoCompra.COMPLETADA)
                .count();
    }

    private static boolean esActiva(Compra c) {
        return c.getEstado() == EstadoCompra.PENDIENTE
                || c.getEstado() == EstadoCompra.COMPLETADA;
    }

    @Override
    public boolean existeCompraDeAlguno(UUID usuarioId, Collection<UUID> productoIds) {
        if (productoIds == null || productoIds.isEmpty()) {
            return false;
        }
        return compras.values().stream()
                .anyMatch(c -> c.getUsuarioId().equals(usuarioId)
                        && productoIds.contains(c.getProductoId())
                        && esActiva(c));
    }
}