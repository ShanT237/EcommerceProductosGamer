package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Caso de uso: NotificarPreventasDisponibles.
 * Cuando llega la fecha de lanzamiento de una preventa, avisa a los compradores que tienen ese
 * producto en su lista de deseos. Es idempotente: ejecutarlo varias veces el mismo día (o tras un
 * reinicio) no duplica avisos, porque el notificador no repite PREVENTA_DISPONIBLE por usuario y producto.
 * Devuelve cuántas preventas se procesaron.
 */
@Service
@RequiredArgsConstructor
public class NotificarPreventasDisponiblesUseCase {

    private final PreventaRepository preventaRepository;
    private final ProductoRepository productoRepository;
    private final NotificadorEventos notificador;
    private final Clock clock;

    @Transactional
    public int ejecutar() {
        LocalDate hoy = LocalDate.now(clock);
        List<Preventa> lanzadas = preventaRepository.obtenerLanzadasEn(hoy);
        int procesadas = 0;
        for (Preventa preventa : lanzadas) {
            var producto = productoRepository.obtenerPorId(preventa.getProductoId())
                    .filter(p -> !p.isEliminado());
            if (producto.isPresent()) {
                notificador.preventaDisponible(producto.get(), preventa);
                procesadas++;
            }
        }
        return procesadas;
    }
}
