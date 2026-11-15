package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ConfirmarPedido.
 * Confirma el pago de todas las líneas del pedido en una sola operación. Primero verifica
 * que el pedido existe, es del usuario y que TODAS sus líneas siguen PENDIENTES; solo
 * entonces cambia el estado, así que nunca queda un pedido a medias.
 */
@Service
@RequiredArgsConstructor
public class ConfirmarPedidoUseCase {

    private final CompraRepository compraRepository;

    @Transactional
    public List<Compra> ejecutar(UUID pedidoId, UUID usuarioId) {
        List<Compra> compras = compraRepository.obtenerPorPedidoId(pedidoId);
        if (compras.isEmpty()) {
            throw new NoSuchElementException("El pedido no existe");
        }
        compras.forEach(compra -> compra.asegurarPerteneceA(usuarioId));
        if (!compras.stream().allMatch(Compra::estaPendiente)) {
            throw new ReglaDominioException("Solo se puede confirmar un pedido con todas sus líneas pendientes");
        }

        compras.forEach(Compra::confirmar);
        compras.forEach(compraRepository::guardar);
        return compras;
    }
}
