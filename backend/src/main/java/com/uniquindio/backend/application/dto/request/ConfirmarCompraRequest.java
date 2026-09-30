package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: Compra.confirmar()
public record ConfirmarCompraRequest(
        @NotNull(message = "La compra es obligatoria")
        UUID idCompra,

        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId
) {}