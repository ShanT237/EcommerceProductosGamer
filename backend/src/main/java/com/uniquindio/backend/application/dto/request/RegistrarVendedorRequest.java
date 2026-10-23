package com.uniquindio.backend.application.dto.request;

import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import jakarta.validation.constraints.*;

// Mapea a: Vendedor.registrar(...)
public record RegistrarVendedorRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo,

        @NotNull(message = "El tipo de vendedor es obligatorio")
        TipoVendedor tipo,

        @NotBlank(message = "El documento es obligatorio")
        @Size(max = 30, message = "El documento no puede superar los 30 caracteres")
        String documento,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasena
) {}