package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: PUT /api/productos/{id}/precio
public record ModificarProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        double precio
) {}