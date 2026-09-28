package com.uniquindio.backend.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmarCompraRequestDTO(
        @NotNull(message = "El id de la compra es obligatorio")
        UUID idCompra,

        @NotNull(message = "El id del usuario es obligatorio")
        UUID usuarioId
) {
}