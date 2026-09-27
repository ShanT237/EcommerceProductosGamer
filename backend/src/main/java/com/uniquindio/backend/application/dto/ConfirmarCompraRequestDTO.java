package com.uniquindio.backend.application.dto;

import java.util.UUID;

public record ConfirmarCompraRequestDTO(
        UUID idCompra,
        UUID usuarioId
) {
}
 