package com.uniquindio.backend.application.usecase;

import java.util.UUID;

/**
 * Una línea del pedido (carrito): qué producto y cuántas unidades.
 * Es el dato de entrada de CrearPedidoUseCase; no depende de la capa REST.
 */
public record ItemPedido(UUID productoId, int cantidad) {
}
