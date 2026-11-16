package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ModificarPreventa.
 * El vendedor dueño del producto cambia la fecha de lanzamiento y la descripción de una preventa
 * que todavía no se ha lanzado. Mismas reglas que al programarla: vendedor activo, producto suyo y
 * no eliminado, y fecha estrictamente futura según la fecha del servidor.
 */
@Service
@RequiredArgsConstructor
public class ModificarPreventaUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    @Transactional
    public Preventa ejecutar(UUID preventaId, UUID vendedorId, LocalDate fechaLanzamiento, String descripcion) {
        Preventa preventa = preventaRepository.obtenerPorId(preventaId)
                .orElseThrow(() -> new NoSuchElementException("La preventa no existe"));

        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.validarPuedePublicar();

        Producto producto = productoRepository.obtenerPorId(preventa.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);
        producto.asegurarNoEliminado();

        preventa.reprogramar(fechaLanzamiento, descripcion, LocalDate.now(clock));

        preventaRepository.guardar(preventa);
        return preventa;
    }
}
