package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Positive;

// Mapea a: RedimirPuntosUseCase.ejecutar(usuarioId, cantidad)
// El usuarioId sale del token (Authentication), no del body.
public record RedimirPuntosRequest(
        @Positive(message = "La cantidad de puntos a redimir debe ser mayor a cero")
        int cantidad
) {}