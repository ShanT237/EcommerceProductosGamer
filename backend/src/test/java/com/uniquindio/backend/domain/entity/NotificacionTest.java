package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NotificacionTest {

    private Notificacion notificacionDe(UUID usuarioId) {
        return Notificacion.reconstruir(UUID.randomUUID(), usuarioId, UUID.randomUUID(),
                TipoNotificacion.DESCUENTO, "Bajó de precio", LocalDateTime.now(), false);
    }

    @Test
    void asegurarPerteneceARechazaAOtroUsuario() {
        Notificacion notificacion = notificacionDe(UUID.randomUUID());

        assertThrows(ReglaDominioException.class,
                () -> notificacion.asegurarPerteneceA(UUID.randomUUID()));
        assertFalse(notificacion.isLeida());
    }

    @Test
    void asegurarPerteneceAAceptaAlDueno() {
        UUID usuarioId = UUID.randomUUID();
        Notificacion notificacion = notificacionDe(usuarioId);

        assertDoesNotThrow(() -> notificacion.asegurarPerteneceA(usuarioId));
    }
}
