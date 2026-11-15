package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: ConsultarPromocionesVigentes.
 * Para los productos que se van a mostrar (catálogo, favoritos) devuelve la promoción de temporada
 * vigente hoy de cada uno, de modo que el comprador vea el mismo precio que se le cobrará al reservar
 * (RealizarCompraUseCase aplica la misma promoción). Los productos sin promoción no aparecen en el mapa.
 */
@Service
@RequiredArgsConstructor
public class ConsultarPromocionesVigentesUseCase {

    private final PromocionRepository promocionRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Map<UUID, Promocion> ejecutar(Collection<UUID> productoIds) {
        if (productoIds.isEmpty()) {
            return Map.of();
        }
        return promocionRepository.obtenerVigentesPorProductoIds(productoIds, LocalDate.now(clock));
    }
}
