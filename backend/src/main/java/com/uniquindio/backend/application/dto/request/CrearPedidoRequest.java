package com.uniquindio.backend.application.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

// Mapea a: CrearPedidoUseCase.ejecutar(usuarioId, items)
// El usuarioId sale del token (Authentication), no del body.
public record CrearPedidoRequest(
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        @Size(max = 50, message = "Un pedido admite como máximo 50 productos")
        List<@NotNull(message = "Cada línea del pedido es obligatoria") @Valid ItemPedidoRequest> items
) {}
