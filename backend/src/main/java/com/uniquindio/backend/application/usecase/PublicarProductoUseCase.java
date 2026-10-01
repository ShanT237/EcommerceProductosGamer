package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: PublicarProducto (CU-04).
 * Orquesta la publicación: obtiene el vendedor, delega en el dominio las reglas
 * y persiste el producto.
 */
@Service
@RequiredArgsConstructor
public class PublicarProductoUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;

    public Producto ejecutar(UUID id, UUID vendedorId, String nombre, String descripcion,
                             double precio, int stock, Gama gama, Exclusividad exclusividad) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));

        vendedor.validarPuedePublicar();

        Producto producto = new Producto(id, vendedor.getId(), nombre, descripcion,
                precio, stock, gama, exclusividad);

        productoRepository.guardar(producto);

        return producto;
    }
}
