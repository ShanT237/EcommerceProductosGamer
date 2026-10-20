package com.uniquindio.backend.application.dto.response;

import java.util.Map;

// Usado en: CU-35 Métricas de Reseñas (distribucion: estrellas 1..5 -> cantidad)
public record MetricasResenasResponse(double promedio, long total, Map<Integer, Long> distribucion,
                                      long positivas, long negativas) {}
