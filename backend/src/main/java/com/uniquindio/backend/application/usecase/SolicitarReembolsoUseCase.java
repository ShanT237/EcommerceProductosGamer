package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Caso de uso: SolicitarReembolso.
 * Procesa el reembolso de una compra completada dentro del plazo legal/comercial,
 * actualiza el estado a REEMBOLSADA y restituye el stock retenido al producto.
 */
@Service
public class SolicitarReembolsoUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    public SolicitarReembolsoUseCase(CompraRepository compraRepository, ProductoRepository productoRepository) {
        this.compraRepository = compraRepository;
        this.productoRepository = productoRepository;
    }

    public Compra ejecutar(UUID compraId, UUID usuarioId, LocalDate fechaSolicitud, int plazoDias) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));

        if (!compra.getUsuarioId().equals(usuarioId)) {
            throw new ReglaDominioException("La compra no pertenece a este usuario");
        }

        // El dominio valida plazo y si fue descargado
        compra.solicitarReembolso(fechaSolicitud, plazoDias);

        // Restituir stock al producto
        Producto producto = productoRepository.obtenerPorId(compra.getProductoId())
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.aumentarStock(compra.getCantidad());

        compraRepository.guardar(compra);
        productoRepository.guardar(producto);

        return compra;
    }
}
