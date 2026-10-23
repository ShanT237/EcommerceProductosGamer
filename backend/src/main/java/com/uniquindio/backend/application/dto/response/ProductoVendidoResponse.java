package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: CU-30 Reporte de Productos Más Vendidos
public record ProductoVendidoResponse(UUID productoId, String nombre, long unidades, double calificacionPromedio) {}
