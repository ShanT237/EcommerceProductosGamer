package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CancelarPreventa.
 * El vendedor dueño del producto cancela una preventa que todavía no se ha lanzado. La preventa se
 * elimina: el producto deja de estar restringido por fecha (los compradores pueden comprarlo desde
 * ese momento) y el vendedor podrá programar otra preventa para él más adelante.
 */
@Service
@RequiredArgsConstructor
public class CancelarPreventaUseCase {

    private final ProductoRepository productoRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    @Transactional
    public void ejecutar(UUID preventaId, UUID vendedorId) {
        Preventa preventa = preventaRepository.obtenerPorId(preventaId)
                .orElseThrow(() -> new NoSuchElementException("La preventa no existe"));
        Producto producto = productoRepository.obtenerPorId(preventa.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        preventa.asegurarNoLanzada(LocalDate.now(clock), "cancelar");

        preventaRepository.eliminar(preventaId);
    }
}
