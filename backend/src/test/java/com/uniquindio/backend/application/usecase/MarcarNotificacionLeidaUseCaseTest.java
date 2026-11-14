package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarcarNotificacionLeidaUseCaseTest {

    private NotificacionRepositoryEnMemoria notificacionRepository;
    private MarcarNotificacionLeidaUseCase useCase;
    private UUID usuarioId;
    private Notificacion notificacion;

    @BeforeEach
    void setUp() {
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        useCase = new MarcarNotificacionLeidaUseCase(notificacionRepository);

        usuarioId = UUID.randomUUID();
        notificacion = Notificacion.reconstruir(UUID.randomUUID(), usuarioId, UUID.randomUUID(),
                TipoNotificacion.DESCUENTO, "Bajó de precio", LocalDateTime.now(), false);
        notificacionRepository.guardar(notificacion);
    }

    @Test
    void marcaLeidaYGuarda() {
        Notificacion resultado = useCase.ejecutar(notificacion.getId(), usuarioId);

        assertTrue(resultado.isLeida());
        assertTrue(notificacionRepository.obtenerPorId(notificacion.getId()).orElseThrow().isLeida());
    }

    @Test
    void ajenaLanzaReglaDominioYNoCambia() {
        UUID otroUsuario = UUID.randomUUID();

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(notificacion.getId(), otroUsuario));

        assertFalse(notificacionRepository.obtenerPorId(notificacion.getId()).orElseThrow().isLeida());
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), usuarioId));
    }
}
