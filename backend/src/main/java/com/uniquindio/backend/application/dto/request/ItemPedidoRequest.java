package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// Una línea de POST /api/pedidos. Mapea a: ItemPedido(productoId, cantidad)
public record ItemPedidoRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}
