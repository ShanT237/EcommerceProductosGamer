package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: SolicitarReembolso.
 * Orquesta el reembolso de una compra completada y la restitución de stock.
 */
@Service
@RequiredArgsConstructor
public class SolicitarReembolsoUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    public Compra ejecutar(UUID compraId, UUID usuarioId, LocalDate fechaSolicitud, int plazoDias) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));
        compra.asegurarPerteneceA(usuarioId);

        Producto producto = productoRepository.obtenerPorId(compra.getProductoId())
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        compra.solicitarReembolso(fechaSolicitud, plazoDias);
        producto.aumentarStock(compra.getCantidad());

        compraRepository.guardar(compra);
        productoRepository.guardar(producto);

        return compra;
    }
}
