package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.UUID;

// Mapea a: new Compra(...) / Producto.reducirStock()
public record RealizarCompraRequest(
        @NotNull(message = "El usuario es obligatorio")
        UUID usuarioId,

        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @Positive(message = "La cantidad debe ser mayor a cero")
        int cantidad
) {}