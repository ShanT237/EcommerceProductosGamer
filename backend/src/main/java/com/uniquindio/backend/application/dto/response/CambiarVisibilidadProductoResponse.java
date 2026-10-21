package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: PUT /api/productos/{id}/visibilidad
public record CambiarVisibilidadProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        boolean pausado
) {}