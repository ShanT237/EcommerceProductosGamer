package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ModificarProducto (CU-05).
 * El vendedor dueño actualiza el precio de su producto; la regla (precio positivo,
 * producto no eliminado) la aplica la entidad.
 */
@Service
@RequiredArgsConstructor
public class ModificarProductoUseCase {

    private final ProductoRepository productoRepository;

    public Producto ejecutar(UUID productoId, UUID vendedorId, double nuevoPrecio) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        producto.cambiarPrecio(nuevoPrecio);

        productoRepository.guardar(producto);

        return producto;
    }
}
