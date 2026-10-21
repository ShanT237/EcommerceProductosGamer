package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: AlertasStockBajo (CU-34).
 * Productos no eliminados del vendedor con stock menor o igual al umbral; los agotados primero.
 */
@Service
@RequiredArgsConstructor
public class AlertasStockBajoUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;

    public List<Producto> ejecutar(UUID vendedorId, int umbral) {
        if (umbral <= 0) {
            throw new ReglaDominioException("El umbral de stock debe ser positivo");
        }
        vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));

        return productoRepository.obtenerTodos().stream()
                .filter(p -> p.getVendedorId().equals(vendedorId))
                .filter(p -> !p.isEliminado())
                .filter(p -> p.getStock() <= umbral)
                .sorted(Comparator.comparingInt(Producto::getStock).thenComparing(Producto::getNombre))
                .toList();
    }
}
