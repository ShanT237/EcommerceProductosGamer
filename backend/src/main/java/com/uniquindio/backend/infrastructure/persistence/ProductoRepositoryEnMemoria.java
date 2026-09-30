package com.uniquindio.backend.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProductoRepositoryEnMemoria implements ProductoRepository {

    private final Map<UUID, Producto> productos = new HashMap<>();

    @Override
    public Optional<Producto> obtenerPorId(UUID id) {
        return Optional.ofNullable(productos.get(id));
    }

    @Override
    public void guardar(Producto producto) {
        productos.put(producto.getId(), producto);
    }
}