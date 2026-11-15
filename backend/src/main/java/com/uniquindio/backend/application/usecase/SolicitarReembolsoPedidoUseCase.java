package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** Reembolsa todas las líneas de un pedido o combo como una sola operación. */
@Service
@RequiredArgsConstructor
public class SolicitarReembolsoPedidoUseCase {

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final Clock clock;

    @Transactional
    public List<Compra> ejecutar(UUID pedidoId, UUID usuarioId) {
        List<Compra> compras = compraRepository.obtenerPorPedidoId(pedidoId);
        if (compras.isEmpty()) {
            throw new NoSuchElementException("El pedido no existe");
        }
        compras.forEach(compra -> compra.asegurarPerteneceA(usuarioId));

        LocalDateTime ahora = LocalDateTime.now(clock);
        List<Producto> productos = new ArrayList<>();
        for (Compra compra : compras) {
            compra.validarSolicitudReembolso(ahora);
            productos.add(productoRepository.obtenerPorId(compra.getProductoId())
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe")));
        }

        for (int i = 0; i < compras.size(); i++) {
            Compra compra = compras.get(i);
            compra.solicitarReembolso(ahora);
            productos.get(i).aumentarStock(compra.getCantidad());
        }

        compras.forEach(compraRepository::guardar);
        productos.forEach(productoRepository::guardar);
        return compras;
    }
}
