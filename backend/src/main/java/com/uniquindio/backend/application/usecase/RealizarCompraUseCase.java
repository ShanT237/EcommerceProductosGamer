package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: RealizarCompra.
 * Recibe la intención (quién compra, qué producto, cuánto), usa los
 * repositorios para obtener/guardar datos, e invoca el comportamiento
 * del dominio (Producto, Compra) para que las reglas se apliquen solas.
 * No contiene ningún if de decisión de negocio: solo coordina.
 */
public class RealizarCompraUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;

    public RealizarCompraUseCase(ProductoRepository productoRepository, CompraRepository compraRepository) {
        this.productoRepository = productoRepository;
        this.compraRepository = compraRepository;
    }

    public Compra ejecutar(UUID id, UUID usuarioId, UUID productoId, int cantidad, LocalDate fecha) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        boolean usuarioYaTieneEsteProducto = compraRepository.existeCompraDe(usuarioId, productoId);

        producto.validarCompraExclusiva(usuarioYaTieneEsteProducto); // Regla 9
        producto.reducirStock(cantidad);                             // Regla 1

        Compra compra = new Compra(id, usuarioId, productoId, cantidad, fecha);

        productoRepository.guardar(producto);
        compraRepository.guardar(compra);

        return compra;
    }
}