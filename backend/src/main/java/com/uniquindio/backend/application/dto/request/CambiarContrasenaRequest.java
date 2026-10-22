package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Mapea a: CambiarContrasenaUseCase.ejecutar(usuarioId, contrasenaActual, contrasenaNueva)
// El usuarioId viaja en la URL: PUT /api/usuarios/{id}/contrasena
// La fortaleza (mayúscula y número) la valida el dominio (Contrasena.validarFortaleza) -> 400.
public record CambiarContrasenaRequest(
        @NotBlank(message = "La contraseña actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La contraseña nueva es obligatoria")
        @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
        String contrasenaNueva
) {}