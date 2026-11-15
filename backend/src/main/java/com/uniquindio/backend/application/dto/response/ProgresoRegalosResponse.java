package com.uniquindio.backend.application.dto.response;

// Usado en: GET /api/regalos/progreso
// Cuántas compras completadas lleva el comprador y cuántos regalos puede elegir todavía.
public record ProgresoRegalosResponse(
        long comprasCompletadas,
        int comprasPorRegalo,
        long regalosAsignados,
        long regalosDisponibles,
        int comprasParaSiguiente
) {}
