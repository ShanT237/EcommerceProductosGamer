package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ConfigurarDescuentoPromocional (CU-22).
 * El vendedor dueño programa un descuento con vigencia; no puede solaparse con otra
 * promoción del mismo producto. El precio base no cambia (a diferencia de AplicarDescuentoUseCase).
 */
@Service
@RequiredArgsConstructor
public class ConfigurarDescuentoPromocionalUseCase {

    private final ProductoRepository productoRepository;
    private final PromocionRepository promocionRepository;

    @Transactional
    public Promocion ejecutar(UUID productoId, UUID vendedorId, double porcentaje,
                              LocalDate desde, LocalDate hasta) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPerteneceA(vendedorId);
        producto.asegurarNoEliminado();

        Promocion promocion = Promocion.programar(UUID.randomUUID(), productoId, porcentaje, desde, hasta);

        boolean solapada = promocionRepository.obtenerPorProductoId(productoId).stream()
                .anyMatch(existente -> existente.seSolapaCon(desde, hasta));
        if (solapada) {
            throw new ReglaDominioException("El producto ya tiene una promoción en ese rango de fechas");
        }

        promocionRepository.guardar(promocion);

        return promocion;
    }
}
