package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EditarInformacionProducto (CU-19).
 * El vendedor dueño actualiza nombre y descripción; las validaciones las aplica la entidad.
 */
@Service
@RequiredArgsConstructor
public class EditarInformacionProductoUseCase {

    private final ProductoRepository productoRepository;

    public Producto ejecutar(UUID productoId, UUID vendedorId, String nombre, String descripcion) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        producto.editarInformacion(nombre, descripcion);

        productoRepository.guardar(producto);

        return producto;
    }
}
