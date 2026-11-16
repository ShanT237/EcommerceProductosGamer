package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ActualizarStockProducto.
 * El vendedor dueño fija el stock disponible de su producto (p. ej. al reponer un producto agotado).
 * Si el producto estaba agotado y vuelve a tener unidades, se avisa a quienes lo tienen en su wishlist.
 */
@Service
@RequiredArgsConstructor
public class ActualizarStockProductoUseCase {

    private final ProductoRepository productoRepository;
    private final NotificadorEventos notificador;

    @Transactional
    public Producto ejecutar(UUID productoId, UUID vendedorId, int nuevoStock) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        int stockAnterior = producto.getStock();
        producto.establecerStock(nuevoStock);

        productoRepository.guardar(producto);
        notificador.stockRepuesto(producto, stockAnterior);

        return producto;
    }
}
