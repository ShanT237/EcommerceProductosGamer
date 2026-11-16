package com.uniquindio.backend.infrastructure.scheduling;

import com.uniquindio.backend.application.usecase.NotificarPreventasDisponiblesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara el aviso de preventas lanzadas: todos los días a las 00:05 y una vez al arrancar la
 * aplicación (por si estuvo apagada a esa hora). El caso de uso es idempotente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PreventasScheduler {

    private final NotificarPreventasDisponiblesUseCase useCase;

    @Scheduled(cron = "0 5 0 * * *")
    public void notificarDiario() {
        ejecutar();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        ejecutar();
    }

    private void ejecutar() {
        try {
            int procesadas = useCase.ejecutar();
            log.info("Preventas lanzadas hoy notificadas: {}", procesadas);
        } catch (RuntimeException e) {
            log.warn("No se pudieron notificar las preventas lanzadas", e);
        }
    }
}
