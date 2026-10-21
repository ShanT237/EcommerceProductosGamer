package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: PUT /api/productos/{id}/informacion
public record EditarInformacionProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        String descripcion
) {}