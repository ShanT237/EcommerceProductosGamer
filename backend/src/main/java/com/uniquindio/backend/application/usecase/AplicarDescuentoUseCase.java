package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: AplicarDescuento (CU-22).
 * El vendedor dueño rebaja el precio de su producto y se notifica a las wishlists
 * que lo contienen.
 */
@Service
@RequiredArgsConstructor
public class AplicarDescuentoUseCase {

    private final ProductoRepository productoRepository;
    private final NotificarDescuentoWishlistUseCase notificarUseCase;

    @Transactional
    public Producto ejecutar(UUID productoId, UUID vendedorId, double porcentaje) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        double precioAnterior = producto.getPrecio();
        producto.aplicarDescuento(porcentaje);
        productoRepository.guardar(producto);

        notificarUseCase.ejecutar(productoId, precioAnterior);
        return producto;
    }
}