package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

// Mapea a: ProgramarPreventaUseCase.ejecutar(vendedorId, productoId, fechaLanzamiento, descripcion)
// La fecha va en formato ISO ("2026-12-01") y debe ser futura (lo valida el dominio con la fecha del servidor).
public record ProgramarPreventaRequest(
        @NotNull(message = "El vendedor es obligatorio")
        UUID vendedorId,

        @NotNull(message = "El producto es obligatorio")
        UUID productoId,

        @NotNull(message = "La fecha de lanzamiento es obligatoria")
        LocalDate fechaLanzamiento,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion
) {}