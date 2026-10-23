package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Mapea a: EditarInformacionProductoUseCase.ejecutar(productoId, vendedorId, nombre, descripcion)
// El vendedorId sale del token (Authentication), no del body.
public record EditarInformacionProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion
) {}