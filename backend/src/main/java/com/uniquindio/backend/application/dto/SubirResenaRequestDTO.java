package com.uniquindio.backend.application.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SubirResenaRequestDTO(
        String idResena,

        @NotNull(message = "El id de la compra es obligatorio")
        UUID idCompra,

        @NotNull(message = "El id del usuario es obligatorio")
        UUID idUsuario,

        @Min(value = 1, message = "La calificación mínima es 1")
        @Max(value = 5, message = "La calificación máxima es 5")
        int valorCalificacion,

        @NotBlank(message = "El comentario no puede estar vacío")
        String comentario
) {
}