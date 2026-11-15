package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Mapea a: CrearCuponUseCase.ejecutar(id, vendedorId, codigo, porcentaje, vence, usosMaximos)
// El vendedorId sale del token (Authentication) y el id lo genera el controller; no vienen en el body.
public record CrearCuponRequest(
        @NotBlank(message = "El código del cupón es obligatorio")
        @Size(max = 50, message = "El código admite como máximo 50 caracteres")
        String codigo,

        @Positive(message = "El porcentaje debe ser mayor a 0")
        @DecimalMax(value = "99.99", message = "El porcentaje debe ser menor que 100")
        double porcentaje,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate vence,

        @Positive(message = "Los usos máximos deben ser positivos")
        int usosMaximos
) {}
