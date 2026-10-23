package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.*;

import java.util.UUID;

// Mapea a: SubirResenaUseCase.ejecutar(idCompra, idUsuario, valorCalificacion, comentario)
// El idUsuario sale del token (Authentication), no del body.
public record SubirResenaRequest(
        @NotNull(message = "La compra es obligatoria")
        UUID idCompra,

        @Min(value = 1, message = "La calificación mínima es 1 estrella")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas")
        int valorCalificacion,

        @NotBlank(message = "El comentario es obligatorio")
        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario
) {}