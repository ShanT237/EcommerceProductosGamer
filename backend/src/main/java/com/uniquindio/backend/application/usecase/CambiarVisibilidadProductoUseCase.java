package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ActivarPausarPublicacion (CU-21).
 * El vendedor dueño alterna la visibilidad del producto sin borrarlo.
 */
@Service
@RequiredArgsConstructor
public class CambiarVisibilidadProductoUseCase {

    private final ProductoRepository productoRepository;

    public Producto ejecutar(UUID productoId, UUID vendedorId, boolean activo) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        if (activo) {
            producto.activar();
        } else {
            producto.pausar();
        }

        productoRepository.guardar(producto);

        return producto;
    }
}
