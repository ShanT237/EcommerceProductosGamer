package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: FiltrarYBuscarProductos (CU-20).
 * Solo lista productos visibles (no eliminados ni pausados). Con soloPreventa devuelve
 * únicamente los que tienen una preventa cuya fecha de lanzamiento aún no llega.
 */
@Service
@RequiredArgsConstructor
public class BuscarProductosUseCase {

    private final ProductoRepository productoRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    public List<Producto> ejecutar(FiltroProducto filtro) {
        LocalDate hoy = LocalDate.now(clock);
        return productoRepository.obtenerTodos().stream()
                .filter(Producto::estaVisible)
                .filter(filtro::coincide)
                .filter(p -> !filtro.soloPreventa() || estaEnPreventa(p, hoy))
                .toList();
    }

    private boolean estaEnPreventa(Producto producto, LocalDate hoy) {
        return preventaRepository.obtenerPorProductoId(producto.getId())
                .map(preventa -> !preventa.estaDisponible(hoy))
                .orElse(false);
    }
}
