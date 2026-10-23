package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

// Mapea a: ValorarVendedorUseCase.ejecutar(compraId, usuarioId, estrellas, comentario)
// El usuarioId sale del token (Authentication), no del body.
// El comentario es opcional (máximo 500 caracteres).
public record ValorarVendedorRequest(
        @NotNull(message = "La compra es obligatoria")
        UUID compraId,

        @Min(value = 1, message = "La calificación mínima es 1 estrella")
        @Max(value = 5, message = "La calificación máxima es 5 estrellas")
        int estrellas,

        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario
) {}