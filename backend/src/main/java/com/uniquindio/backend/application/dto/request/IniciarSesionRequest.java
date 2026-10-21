package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;

// Mapea a: IniciarSesionUseCase.ejecutar(correo, contrasena)
public record IniciarSesionRequest(
        @NotBlank(message = "El correo es obligatorio")
        String correo,

        @NotBlank(message = "La contraseña es obligatoria")
        String contrasena
) {}
