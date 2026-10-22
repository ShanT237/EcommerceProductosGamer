package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.application.usecase.ReporteVentasVendedorUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Limitación: el {@code vendedorId} viaja como parámetro; el JWT valida la sesión pero
 * no se cruza con el vendedor consultado.
 */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteVentasVendedorUseCase reporteVentasVendedorUseCase;

    public ReporteController(ReporteVentasVendedorUseCase reporteVentasVendedorUseCase) {
        this.reporteVentasVendedorUseCase = reporteVentasVendedorUseCase;
    }

    // CU-29 Reporte de Ventas por Vendedor
    @GetMapping("/ventas")
    public ResponseEntity<ReporteVentasResponse> ventasPorVendedor(@RequestParam UUID vendedorId) {
        return ResponseEntity.ok(reporteVentasVendedorUseCase.ejecutar(vendedorId)); // 200 OK
    }
}