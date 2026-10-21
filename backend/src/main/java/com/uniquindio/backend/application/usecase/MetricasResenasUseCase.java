package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.MetricasResenasResponse;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.ResenaRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Caso de uso: MetricasResenas (CU-35).
 * Satisfacción de los clientes de un vendedor: promedio, distribución por estrellas y
 * conteo de reseñas positivas (4-5) y negativas (1-2).
 */
@Service
@RequiredArgsConstructor
public class MetricasResenasUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final ResenaRepository resenaRepository;

    public MetricasResenasResponse ejecutar(UUID vendedorId) {
        vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));

        List<Resena> resenas = productoRepository.obtenerTodos().stream()
                .filter(p -> p.getVendedorId().equals(vendedorId))
                .map(Producto::getId)
                .flatMap(id -> resenaRepository.obtenerPorProductoId(id.toString()).stream())
                .toList();

        Map<Integer, Long> distribucion = new TreeMap<>();
        for (int estrellas = 1; estrellas <= 5; estrellas++) {
            int e = estrellas;
            distribucion.put(e, resenas.stream().filter(r -> r.getCalificacion().estrellas() == e).count());
        }
        double promedio = resenas.stream().mapToInt(r -> r.getCalificacion().estrellas()).average().orElse(0);
        long positivas = resenas.stream().filter(r -> r.getCalificacion().esPositiva()).count();
        long negativas = resenas.stream().filter(r -> r.getCalificacion().esNegativa()).count();

        return new MetricasResenasResponse(promedio, resenas.size(), distribucion, positivas, negativas);
    }
}
