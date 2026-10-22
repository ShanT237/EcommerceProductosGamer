package com.uniquindio.backend.application.dto.request;

import jakarta.validation.constraints.Positive;

// Mapea a: GestionarMetasRecompensaUseCase.actualizarUmbral(metaId, nuevoUmbral)
public record ActualizarUmbralMetaRequest(
        @Positive(message = "El umbral de compras debe ser mayor a cero")
        int nuevoUmbral
) {}