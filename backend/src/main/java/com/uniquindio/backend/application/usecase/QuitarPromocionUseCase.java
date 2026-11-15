package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: QuitarPromocion.
 * El vendedor dueño elimina una promoción de temporada (vigente, futura o vencida).
 * El precio base del producto no cambia, porque la promoción nunca lo tocó.
 */
@Service
@RequiredArgsConstructor
public class QuitarPromocionUseCase {

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;

    @Transactional
    public void ejecutar(UUID promocionId, UUID vendedorId) {
        Promocion promocion = promocionRepository.obtenerPorId(promocionId)
                .orElseThrow(() -> new NoSuchElementException("La promoción no existe"));
        Producto producto = productoRepository.obtenerPorId(promocion.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);

        promocionRepository.eliminar(promocionId);
    }
}