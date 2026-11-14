package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: QuitarDescuento (CU-22).
 * El vendedor dueño retira el descuento inmediato y el producto recupera su precio anterior.
 * No se notifica a las wishlists: solo se avisa cuando el precio baja.
 */
@Service
@RequiredArgsConstructor
public class QuitarDescuentoUseCase {

    private final ProductoRepository productoRepository;

    @Transactional
    public Producto ejecutar(UUID productoId, UUID vendedorId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        producto.quitarDescuento();
        productoRepository.guardar(producto);

        return producto;
    }
}
