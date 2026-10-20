package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.ResenaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: ReporteProductosMasVendidos (CU-30).
 * Escalafón por unidades vendidas en compras COMPLETADAS (desempate: calificación promedio,
 * luego nombre). Sin vendedor lista todo el marketplace; los productos sin ventas no aparecen.
 */
@Service
@RequiredArgsConstructor
public class ReporteMasVendidosUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final ResenaRepository resenaRepository;

    public List<ProductoVendidoResponse> ejecutar(UUID vendedorIdOpcional, int top) {
        if (top <= 0) {
            throw new ReglaDominioException("El tamaño del escalafón debe ser positivo");
        }

        Map<UUID, Integer> unidadesPorProducto = new HashMap<>();
        for (Compra compra : compraRepository.obtenerTodas()) {
            if (compra.estaCompletada()) {
                unidadesPorProducto.merge(compra.getProductoId(), compra.getCantidad(), Integer::sum);
            }
        }

        return productoRepository.obtenerTodos().stream()
                .filter(p -> vendedorIdOpcional == null || p.getVendedorId().equals(vendedorIdOpcional))
                .filter(p -> unidadesPorProducto.containsKey(p.getId()))
                .map(p -> aRespuesta(p, unidadesPorProducto.get(p.getId())))
                .sorted(Comparator.comparingInt(ProductoVendidoResponse::unidades).reversed()
                        .thenComparing(Comparator.comparingDouble(ProductoVendidoResponse::calificacionPromedio).reversed())
                        .thenComparing(ProductoVendidoResponse::nombre))
                .limit(top)
                .toList();
    }

    private ProductoVendidoResponse aRespuesta(Producto producto, int unidades) {
        double promedio = resenaRepository.obtenerPorProductoId(producto.getId().toString()).stream()
                .mapToInt((Resena r) -> r.getCalificacion().estrellas())
                .average()
                .orElse(0);
        return new ProductoVendidoResponse(producto.getId(), producto.getNombre(), unidades, promedio);
    }
}
