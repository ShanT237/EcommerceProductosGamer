package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Caso de uso: ConsultarPromocionesDelVendedor.
 * Todas las promociones de temporada (pasadas, vigentes y futuras) de los productos del vendedor,
 * de la más reciente a la más antigua. No incluye productos eliminados.
 */
@Service
@RequiredArgsConstructor
public class ConsultarPromocionesDelVendedorUseCase {

    public record PromocionDeProducto(Promocion promocion, String productoNombre) {}

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;

    @Transactional(readOnly = true)
    public List<PromocionDeProducto> ejecutar(UUID vendedorId) {
        // Si tu getter de eliminado tiene otro nombre (p. ej. estaEliminado()), ajústalo aquí.
        Map<UUID, Producto> propios = productoRepository.obtenerTodos().stream()
                .filter(p -> vendedorId.equals(p.getVendedorId()) && !p.isEliminado())
                .collect(Collectors.toMap(Producto::getId, Function.identity()));
        if (propios.isEmpty()) {
            return List.of();
        }
        return promocionRepository.obtenerPorProductoIds(propios.keySet()).stream()
                .sorted(Comparator.comparing(Promocion::getDesde).reversed())
                .map(pr -> new PromocionDeProducto(pr, propios.get(pr.getProductoId()).getNombre()))
                .toList();
    }
}