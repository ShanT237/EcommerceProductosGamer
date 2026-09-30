package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EliminarProducto (CU-08).
 * Desactiva un producto preservando el historial de compras (Regla 3).
 * Averigua vía repositorio si el producto tiene compras activas y delega en
 * Producto.eliminar(...) la decisión: con compras activas se rechaza; sin ellas
 * se marca como eliminado lógicamente (nunca se borra físicamente).
 * No contiene ningún if de decisión de negocio: solo coordina.
 */
@Service
public class EliminarProductoUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;

    public EliminarProductoUseCase(ProductoRepository productoRepository,
                                   CompraRepository compraRepository) {
        this.productoRepository = productoRepository;
        this.compraRepository = compraRepository;
    }

    public void ejecutar(UUID productoId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));

        boolean tieneComprasActivas = compraRepository.existeCompraActivaDeProducto(productoId);

        producto.eliminar(tieneComprasActivas); // Regla 3

        productoRepository.guardar(producto);
    }
}