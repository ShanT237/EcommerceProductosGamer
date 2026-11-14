package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarNotificacionesUseCaseTest {

    private NotificacionRepositoryEnMemoria notificacionRepository;
    private ConsultarNotificacionesUseCase useCase;

    @BeforeEach
    void setUp() {
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        useCase = new ConsultarNotificacionesUseCase(notificacionRepository);
    }

    private Notificacion guardar(UUID usuarioId, LocalDateTime fecha) {
        Notificacion n = Notificacion.reconstruir(UUID.randomUUID(), usuarioId, UUID.randomUUID(),
                TipoNotificacion.DESCUENTO, "Bajó de precio", fecha, false);
        notificacionRepository.guardar(n);
        return n;
    }

    @Test
    void devuelveSoloLasDelUsuarioMasRecientePrimero() {
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime base = LocalDateTime.of(2026, 11, 14, 10, 0);
        Notificacion vieja = guardar(usuarioId, base.minusDays(2));
        Notificacion reciente = guardar(usuarioId, base);
        Notificacion media = guardar(usuarioId, base.minusDays(1));
        guardar(UUID.randomUUID(), base); // ajena: no debe aparecer

        List<Notificacion> resultado = useCase.ejecutar(usuarioId);

        assertEquals(List.of(reciente.getId(), media.getId(), vieja.getId()),
                resultado.stream().map(Notificacion::getId).toList());
    }

    @Test
    void sinNotificacionesDevuelveVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }
}
