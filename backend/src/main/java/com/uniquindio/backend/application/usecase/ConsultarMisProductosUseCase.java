package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarMisProductos (panel del vendedor).
 * Devuelve TODOS los productos del vendedor, activos y pausados, para que pueda reactivar
 * los que pausó. Solo excluye los eliminados (eliminación lógica, no se pueden recuperar).
 * A diferencia de BuscarProductosUseCase (catálogo público), no filtra por visibilidad.
 */
@Service
@RequiredArgsConstructor
public class ConsultarMisProductosUseCase {

    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<Producto> ejecutar(UUID vendedorId) {
        return productoRepository.obtenerTodos().stream()
                .filter(p -> vendedorId.equals(p.getVendedorId()) && !p.isEliminado())
                .sorted(Comparator.comparing(Producto::getNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
