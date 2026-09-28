package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Producto;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Producto.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar productos
 */
public interface ProductoRepository {

    Optional<Producto> obtenerPorId(UUID id);

    void guardar(Producto producto);
}