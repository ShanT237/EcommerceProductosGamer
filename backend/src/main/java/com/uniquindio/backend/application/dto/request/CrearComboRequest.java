package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

// Mapea a: CrearComboUseCase.ejecutar(id, vendedorId, nombre, descripcion, descuento, productoIds)
// El vendedorId sale del token (Authentication), no del body.
public record CrearComboRequest(
        @NotBlank(message = "El nombre del combo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @PositiveOrZero(message = "El descuento no puede ser negativo")
        @DecimalMax(value = "99.99", message = "El descuento debe ser menor al 100%")
        double descuento,

        @NotNull(message = "La lista de productos es obligatoria")
        @Size(min = 2, message = "El combo debe incluir al menos dos productos")
        List<UUID> productoIds
) {}