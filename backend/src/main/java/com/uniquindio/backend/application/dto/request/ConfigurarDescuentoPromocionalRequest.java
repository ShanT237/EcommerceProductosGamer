package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.util.UUID;

// Mapea a: ConfigurarDescuentoPromocionalUseCase.ejecutar(productoId, vendedorId, porcentaje, desde, hasta)
// Fechas en formato ISO: "2026-12-01"
public record ConfigurarDescuentoPromocionalRequest(
        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @NotNull(message = "El vendedor es obligatorio")
        UUID vendedorId,

        @Positive(message = "El porcentaje debe ser mayor a cero")
        @DecimalMax(value = "99.99", message = "El descuento debe ser menor al 100%")
        double porcentaje,

        @NotNull(message = "La fecha de inicio es obligatoria")
        LocalDate desde,

        @NotNull(message = "La fecha de fin es obligatoria")
        LocalDate hasta
) {}