package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.MetricasResenasResponse;
import com.uniquindio.backend.application.dto.response.ProductoStockBajoResponse;
import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.dto.response.ReporteGlobalResponse;
import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.AlertasStockBajoUseCase;
import com.uniquindio.backend.application.usecase.MetricasResenasUseCase;
import com.uniquindio.backend.application.usecase.ReporteGlobalUseCase;
import com.uniquindio.backend.application.usecase.ReporteMasVendidosUseCase;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import com.uniquindio.backend.infrastructure.rest.mapper.ProductoMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Limitación: los reportes de vendedor reciben {@code vendedorId} como parámetro y el
 * reporte global es de admin, pero no hay roles. El JWT valida la sesión y nada más.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;
    private final ReporteMasVendidosUseCase reporteMasVendidosUseCase;
    private final AlertasStockBajoUseCase alertasStockBajoUseCase;
    private final MetricasResenasUseCase metricasResenasUseCase;
    private final ReporteGlobalUseCase reporteGlobalUseCase;
    private final ProductoMapper productoMapper;

    public ReporteController(ReporteVentasVendedorUseCase reporteVentasVendedorUseCase,
                             ReporteMasVendidosUseCase reporteMasVendidosUseCase,
                             AlertasStockBajoUseCase alertasStockBajoUseCase,
                             MetricasResenasUseCase metricasResenasUseCase,
                             ReporteGlobalUseCase reporteGlobalUseCase,
                             ProductoMapper productoMapper) {
        this.reporteVentasVendedorUseCase = reporteVentasVendedorUseCase;
        this.reporteMasVendidosUseCase = reporteMasVendidosUseCase;
        this.alertasStockBajoUseCase = alertasStockBajoUseCase;
        this.metricasResenasUseCase = metricasResenasUseCase;
        this.reporteGlobalUseCase = reporteGlobalUseCase;
        this.productoMapper = productoMapper;
    }

    // CU-29 Reporte de Ventas por Vendedor
    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> ventasPorVendedor(@RequestParam UUID vendedorId) {
        return ResponseEntity.ok(reporteVentasVendedorUseCase.ejecutar(vendedorId)); // 200 OK
    }

    // CU-30 Reporte de Productos Más Vendidos (sin vendedorId = todo el marketplace)
    @GetMapping("/mas-vendidos")
    public ResponseEntity<List<ProductoVendidoResponse>> masVendidos(
            @RequestParam(required = false) UUID vendedorId,
            @RequestParam(defaultValue = "10") int top) {
        return ResponseEntity.ok(reporteMasVendidosUseCase.ejecutar(vendedorId, top)); // 200 OK
    }

    // CU-34 Alertas de Stock Bajo (agotados primero)
    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoStockBajoResponse>> stockBajo(
            @RequestParam UUID vendedorId,
            @RequestParam(defaultValue = "5") int umbral) {
        List<ProductoStockBajoResponse> respuesta = alertasStockBajoUseCase.ejecutar(vendedorId, umbral).stream()
                .map(productoMapper::toStockBajoResponse)
                .toList();
        return ResponseEntity.ok(respuesta); // 200 OK
    }

    // CU-35 Métricas de Reseñas (promedio, distribución por estrellas, positivas y negativas)
    @GetMapping("/metricas-resenas")
    public ResponseEntity<MetricasResenasResponse> metricasResenas(@RequestParam UUID vendedorId) {
        return ResponseEntity.ok(metricasResenasUseCase.ejecutar(vendedorId)); // 200 OK
    }

    // CU-36 Reporte Global del Marketplace (compras completadas, ingresos, productos y vendedores activos)
    @GetMapping("/global")
    public ResponseEntity<ReporteGlobalResponse> global() {
        return ResponseEntity.ok(reporteGlobalUseCase.ejecutar()); // 200 OK
    }
}