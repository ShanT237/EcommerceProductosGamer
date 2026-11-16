package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;

import java.time.LocalDate;
import java.util.UUID;

// Usado en: GET /api/productos/mios (listado del propio vendedor).
// Igual que BuscarProductoResponse, más el estado de publicación: aquí SÍ aparecen los pausados.
public record MiProductoResponse(
        UUID id,
        UUID vendedorId,
        String nombre,
        String descripcion,
        double precio,
        int stock,
        Gama gama,
        Exclusividad exclusividad,
        String categoria,
        Double precioOriginal,
        Double porcentajeDescuento,
        Double precioPromocional,
        Double porcentajePromocion,
        LocalDate promocionHasta,
        boolean pausado,              // true = el vendedor lo pausó y no se ve en el catálogo público
        String imagenUrl
) {}
