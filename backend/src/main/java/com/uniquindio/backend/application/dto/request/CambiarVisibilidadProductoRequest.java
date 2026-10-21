package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Mapea a: CambiarVisibilidadProductoUseCase.ejecutar(productoId, vendedorId, activo)
// activo = true -> activar; activo = false -> pausar.
// Es Boolean (no boolean) para que un body sin el campo falle con 400 y no pause por defecto.
public record CambiarVisibilidadProductoRequest(
        @NotNull(message = "El vendedor es obligatorio")
        UUID vendedorId,

        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {}