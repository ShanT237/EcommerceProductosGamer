package com.uniquindio.backend.application.dto.request;

import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: new Producto(...)
public record PublicarProductoRequest(
        @NotNull(message = "El vendedor es obligatorio")
        UUID vendedorId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "La descripción es obligatoria")
        @Size(max = 500, message = "La descripción no puede superar los 500 caracteres")
        String descripcion,

        @Positive(message = "El precio debe ser mayor a cero")
        double precio,

        @PositiveOrZero(message = "El stock no puede ser negativo")
        int stock,

        @NotNull(message = "La gama es obligatoria")
        Gama gama,

        @NotNull(message = "La exclusividad es obligatoria")
        Exclusividad exclusividad
) {}