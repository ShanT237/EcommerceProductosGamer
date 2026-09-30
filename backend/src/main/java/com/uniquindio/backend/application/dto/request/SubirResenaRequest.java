package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: Resena.crear()
public record SubirResenaRequest(
        @NotBlank(message = "El id de la reseña es obligatorio")
        String idResena,

        @NotNull(message = "La compra es obligatoria")
        UUID idCompra,

        @NotNull(message = "El usuario es obligatorio")
        UUID idUsuario,

        @Min(value = 1, message = "La calificación mínima es 1 estrella")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas")
        int valorCalificacion,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario
) {}