package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Mapea a: EditarPerfilUseCase.ejecutar(usuarioId, nombre, correo)
// El usuarioId sale del token: PUT /api/usuarios/me/perfil
public record EditarPerfilRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo
) {}