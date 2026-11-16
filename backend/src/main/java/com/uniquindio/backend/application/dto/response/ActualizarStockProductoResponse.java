package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: PUT /api/productos/{id}/stock
public record ActualizarStockProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        int stock
) {}
