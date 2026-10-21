package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;

import java.util.UUID;

// Usado en: GET /api/productos (un elemento por producto encontrado)
public record BuscarProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        String descripcion,
        double precio,
        int stock,
        Gama gama,
        Exclusividad exclusividad,
        String categoria
) {}