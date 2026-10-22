package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: AsignarRegaloPorMetaUseCase.ejecutar(usuarioId, regaloId)
// El regaloId viaja en la URL: PUT /api/regalos/{id}/asignar
public record AsignarRegaloRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId
) {}