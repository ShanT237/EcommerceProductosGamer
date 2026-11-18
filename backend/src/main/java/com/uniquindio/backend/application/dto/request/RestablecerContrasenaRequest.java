package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Mapea a: RestablecerContrasenaUseCase.ejecutar(token, contrasenaNueva)
public record RestablecerContrasenaRequest(
        @NotBlank(message = "El enlace de recuperación es obligatorio")
        @Size(max = 200, message = "El enlace de recuperación no es válido")
        String token,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String contrasenaNueva
) {}
