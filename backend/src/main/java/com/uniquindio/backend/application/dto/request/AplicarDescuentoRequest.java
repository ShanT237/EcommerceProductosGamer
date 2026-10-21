package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record AplicarDescuentoRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @Positive(message = "El porcentaje debe ser mayor a cero")
        @DecimalMax(value = "99.99", message = "El descuento debe ser menor al 100%")
        double porcentaje
) {}