package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;

// Mapea a: CambiarVisibilidadProductoUseCase.ejecutar(productoId, vendedorId, activo)
// El vendedorId sale del token (Authentication), no del body.
// activo = true -> activar; activo = false -> pausar.
// Es Boolean (no boolean) para que un body sin el campo falle con 400 y no pause por defecto.
public record CambiarVisibilidadProductoRequest(
        @NotNull(message = "El estado activo es obligatorio")
        Boolean activo
) {}