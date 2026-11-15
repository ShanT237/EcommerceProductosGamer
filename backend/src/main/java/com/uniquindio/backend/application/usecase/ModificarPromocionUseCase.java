package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ModificarPromocion.
 * El vendedor dueño cambia porcentaje y vigencia de una promoción de temporada. Mismas reglas que al
 * configurarla (porcentaje válido, fechas coherentes, sin solaparse con otra promoción del producto).
 */
@Service
@RequiredArgsConstructor
public class ModificarPromocionUseCase {

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;

    @Transactional
    public Promocion ejecutar(UUID promocionId, UUID vendedorId, double porcentaje,
                              LocalDate desde, LocalDate hasta) {
        Promocion actual = promocionRepository.obtenerPorId(promocionId)
                .orElseThrow(() -> new NoSuchElementException("La promoción no existe"));
        Producto producto = productoRepository.obtenerPorId(actual.getProductoId())
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);
        producto.asegurarNoEliminado();

        Promocion nueva = actual.reprogramar(porcentaje, desde, hasta);
        nueva.asegurarSinSolapamiento(promocionRepository.obtenerPorProductoId(actual.getProductoId()));

        promocionRepository.guardar(nueva);
        return nueva;
    }
}