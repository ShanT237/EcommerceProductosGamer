package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AplicarDescuentoUseCase {

    private final ProductoRepository productoRepository;
    private final NotificarDescuentoWishlistUseCase notificarUseCase;

    @Transactional
    public Producto ejecutar(UUID productoId, double porcentaje) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));

        double precioAnterior = producto.getPrecio();
        producto.aplicarDescuento(porcentaje);
        productoRepository.guardar(producto);

        notificarUseCase.ejecutar(productoId, precioAnterior);
        return producto;
    }
}