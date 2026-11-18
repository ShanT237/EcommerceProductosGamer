package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Mapea a: SolicitarRecuperacionContrasenaUseCase.ejecutar(correo)
public record SolicitarRecuperacionRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
        String correo
) {}
