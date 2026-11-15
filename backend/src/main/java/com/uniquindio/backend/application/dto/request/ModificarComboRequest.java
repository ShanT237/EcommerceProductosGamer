package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

// Mapea a: ModificarComboUseCase.ejecutar(comboId, vendedorId, nombre, descripcion, descuento)
// El comboId viaja en la URL y el vendedorId sale del token.
public record ModificarComboRequest(
        @NotBlank(message = "El nombre del combo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @PositiveOrZero(message = "El descuento no puede ser negativo")
        @DecimalMax(value = "99.99", message = "El descuento debe ser menor al 100%")
        double descuento
) {}
