package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: CancelarCompra.
 * Procesa la cancelación de una compra PENDIENTE y devuelve las unidades retenidas
 * al inventario del producto.
 */
@Service
public class CancelarCompraUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    public CancelarCompraUseCase(CompraRepository compraRepository, ProductoRepository productoRepository) {
        this.compraRepository = compraRepository;
        this.productoRepository = productoRepository;
    }

    public Compra ejecutar(UUID compraId, UUID usuarioId) {
        Compra compra = compraRepository.obtenerPorId(compraId)
                .orElseThrow(() -> new ReglaDominioException("La compra no existe"));

        if (!compra.getUsuarioId().equals(usuarioId)) {
            throw new ReglaDominioException("La compra no pertenece a este usuario");
        }

        // El dominio valida que la compra esté PENDIENTE y cambia estado a CANCELADA
        compra.cancelar();

        // Restituir stock al producto
        Producto producto = productoRepository.obtenerPorId(compra.getProductoId())
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.aumentarStock(compra.getCantidad());

        compraRepository.guardar(compra);
        productoRepository.guardar(producto);

        return compra;
    }
}
