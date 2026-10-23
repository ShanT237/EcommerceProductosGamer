package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.NoSuchElementException;
import java.util.UUID;


/**
 * Caso de uso: EliminarProducto (CU-06).
 * Orquesta la desactivación de un producto: solo su vendedor dueño puede eliminarlo;
 * consulta si tiene compras activas y delega en Producto.eliminar(...) la decisión
 * de negocio (Regla 3).
 */
@Service
@RequiredArgsConstructor
public class EliminarProductoUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;

    @Transactional
    public void ejecutar(UUID productoId, UUID vendedorId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        boolean tieneComprasActivas = compraRepository.existeCompraActivaDeProducto(productoId);

        producto.eliminar(tieneComprasActivas);

        productoRepository.guardar(producto);
    }
}
