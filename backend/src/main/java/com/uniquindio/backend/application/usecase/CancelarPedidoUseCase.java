package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CancelarPedido.
 * Cancela todas las líneas PENDIENTES del pedido y restituye el stock de cada producto.
 * Verifica todo (existencia, dueño, estado y productos) ANTES de mutar, para no dejar
 * un pedido parcialmente cancelado.
 */
@Service
@RequiredArgsConstructor
public class CancelarPedidoUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;

    @Transactional
    public List<Compra> ejecutar(UUID pedidoId, UUID usuarioId) {
        List<Compra> compras = compraRepository.obtenerPorPedidoId(pedidoId);
        if (compras.isEmpty()) {
            throw new NoSuchElementException("El pedido no existe");
        }
        compras.forEach(compra -> compra.asegurarPerteneceA(usuarioId));
        if (!compras.stream().allMatch(Compra::estaPendiente)) {
            throw new ReglaDominioException("Solo se puede cancelar un pedido con todas sus líneas pendientes");
        }

        List<Producto> productos = new ArrayList<>();
        for (Compra compra : compras) {
            productos.add(productoRepository.obtenerPorId(compra.getProductoId())
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe")));
        }

        for (int i = 0; i < compras.size(); i++) {
            compras.get(i).cancelar();
            productos.get(i).aumentarStock(compras.get(i).getCantidad());
        }

        compras.forEach(compraRepository::guardar);
        productos.forEach(productoRepository::guardar);
        return compras;
    }
}
