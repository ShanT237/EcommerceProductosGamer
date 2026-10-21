package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Caso de uso: ReporteVentasVendedor (CU-29).
 * Balance del vendedor sobre sus compras COMPLETADAS: ingresos, unidades y comisión del
 * marketplace (5%). Un vendedor dado de baja conserva su reporte (historial).
 */
@Service
@RequiredArgsConstructor
public class ReporteVentasVendedorUseCase {

    public static final double PORCENTAJE_COMISION = 5.0;

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;

    public ReporteVentasResponse ejecutar(UUID vendedorId) {
        vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));

        Set<UUID> productosDelVendedor = productoRepository.obtenerTodos().stream()
                .filter(p -> p.getVendedorId().equals(vendedorId))
                .map(Producto::getId)
                .collect(Collectors.toSet());

        double ingresos = 0;
        int unidades = 0;
        for (Compra compra : compraRepository.obtenerTodas()) {
            if (compra.estaCompletada() && productosDelVendedor.contains(compra.getProductoId())) {
                ingresos += compra.getSubtotal().valor();
                unidades += compra.getCantidad();
            }
        }
        return new ReporteVentasResponse(ingresos, unidades, ingresos * PORCENTAJE_COMISION / 100);
    }
}
