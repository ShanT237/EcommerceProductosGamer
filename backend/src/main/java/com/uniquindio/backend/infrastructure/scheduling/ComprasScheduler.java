package com.uniquindio.backend.infrastructure.scheduling;

import com.uniquindio.backend.application.usecase.ExpirarComprasPendientesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Expira las compras PENDIENTES de más de 10 minutos: cada 5 minutos, sin ejecución al arrancar
 * y sin endpoint manual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComprasScheduler {

    private final ExpirarComprasPendientesUseCase useCase;

    @Scheduled(cron = "0 */5 * * * *")
    public void expirarPendientes() {
        try {
            int expiradas = useCase.ejecutar();
            log.info("Compras pendientes expiradas: {}", expiradas);
        } catch (RuntimeException e) {
            log.warn("No se pudieron expirar las compras pendientes", e);
        }
    }
}
