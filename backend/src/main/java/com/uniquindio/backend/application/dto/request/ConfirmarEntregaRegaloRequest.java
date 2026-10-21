package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: ConfirmarEntregaRegaloUseCase.ejecutar(regaloId, usuarioId)
// El regaloId viaja en la URL: PUT /api/regalos/{id}/entrega
public record ConfirmarEntregaRegaloRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId
) {}