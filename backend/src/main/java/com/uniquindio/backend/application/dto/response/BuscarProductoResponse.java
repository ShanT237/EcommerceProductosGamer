package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;

import java.time.LocalDate;
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
        String categoria,
        Double precioOriginal,        // precio antes del descuento; null si no hay descuento activo
        Double porcentajeDescuento,   // porcentaje del descuento activo; null si no hay
        Double precioPromocional,     // precio que se cobrará con la promoción de temporada vigente hoy; null si no hay
        Double porcentajePromocion,   // porcentaje de esa promoción; null si no hay
        LocalDate promocionHasta,     // último día de la promoción (inclusive); null si no hay
        String imagenUrl
) {}
