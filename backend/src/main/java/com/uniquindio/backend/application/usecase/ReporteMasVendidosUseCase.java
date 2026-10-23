package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.port.out.ReporteMasVendidosPort;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ReporteProductosMasVendidos (CU-30).
 * Escalafón por unidades vendidas en compras COMPLETADAS, calculado y ordenado en base de datos.
 * Sin vendedor lista todo el marketplace; los productos sin ventas no aparecen.
 */
@Service
@RequiredArgsConstructor
public class ReporteMasVendidosUseCase {

    private final ReporteMasVendidosPort reporteMasVendidosPort;

    public List<ProductoVendidoResponse> ejecutar(UUID vendedorIdOpcional, int top) {
        if (top <= 0) {
            throw new ReglaDominioException("El tamaño del escalafón debe ser positivo");
        }
        return reporteMasVendidosPort.obtener(vendedorIdOpcional, top);
    }
}
