package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.ReporteMasVendidosUseCase;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Limitación: el {@code vendedorId} viaja como parámetro; el JWT valida la sesión pero
 * no se cruza con el vendedor consultado.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;
    private final ReporteMasVendidosUseCase reporteMasVendidosUseCase;

    public ReporteController(ReporteVentasVendedorUseCase reporteVentasVendedorUseCase,
                             ReporteMasVendidosUseCase reporteMasVendidosUseCase) {
        this.reporteVentasVendedorUseCase = reporteVentasVendedorUseCase;
        this.reporteMasVendidosUseCase = reporteMasVendidosUseCase;
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
}