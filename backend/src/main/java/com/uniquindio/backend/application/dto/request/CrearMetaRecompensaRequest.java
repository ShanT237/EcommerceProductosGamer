package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

// Mapea a: GestionarMetasRecompensaUseCase.crear(id, comprasRequeridas, productoRegaloId)
public record CrearMetaRecompensaRequest(
        @Positive(message = "El umbral de compras debe ser mayor a cero")
        int comprasRequeridas,

        @NotNull(message = "El producto regalo es obligatorio")
        UUID productoRegaloId
) {}