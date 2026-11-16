package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Mapea a: ModificarPreventaUseCase.ejecutar(preventaId, vendedorId, fechaLanzamiento, descripcion)
// El vendedorId sale del token (Authentication) y el id de la preventa de la URL, no del body.
// La fecha va en formato ISO ("2026-12-01") y debe ser futura (lo valida el dominio con la fecha del servidor).
public record ModificarPreventaRequest(
        @NotNull(message = "La fecha de lanzamiento es obligatoria")
        LocalDate fechaLanzamiento,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion
) {}
