package com.uniquindio.backend.application.dto.response;

// Usado en: CU-36 Reporte Global del Marketplace
public record ReporteGlobalResponse(int comprasCompletadas, double ingresos,
                                    int productosActivos, int vendedoresActivos) {}
