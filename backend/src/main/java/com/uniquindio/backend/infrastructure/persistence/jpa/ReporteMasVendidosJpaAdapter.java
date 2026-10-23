package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.application.port.out.ReporteMasVendidosPort;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReporteMasVendidosJpaAdapter implements ReporteMasVendidosPort {

    private final ProductoJpaDataRepository productoDataRepository;
    private final ResenaJpaDataRepository resenaDataRepository;

    @Override
    public List<ProductoVendidoResponse> obtener(UUID vendedorIdOpcional, int top) {
        List<ProductoMasVendidoProjection> ventas = productoDataRepository.buscarMasVendidos(
                vendedorIdOpcional, EstadoCompra.COMPLETADA, PageRequest.of(0, top));
        if (ventas.isEmpty()) {
            return List.of();
        }

        List<String> ids = ventas.stream()
                .map(venta -> venta.getProductoId().toString())
                .toList();
        Map<String, Double> promedios = resenaDataRepository.promediosPorProducto(ids).stream()
                .collect(Collectors.toMap(
                        PromedioResenaProjection::getProductoId,
                        PromedioResenaProjection::getPromedio));

        return ventas.stream()
                .map(venta -> new ProductoVendidoResponse(
                        venta.getProductoId(),
                        venta.getNombre(),
                        venta.getUnidades(),
                        promedios.getOrDefault(venta.getProductoId().toString(), 0.0)))
                .toList();
    }
}
