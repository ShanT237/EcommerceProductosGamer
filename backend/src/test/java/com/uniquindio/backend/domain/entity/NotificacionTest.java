package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void crearPermiteUnAvisoSinProducto() {
        UUID destinatario = UUID.randomUUID();

        Notificacion n = Notificacion.crear(destinatario, null, TipoNotificacion.META_ALCANZADA, "Meta alcanzada");

        assertNull(n.getProductoId());
        assertEquals(destinatario, n.getUsuarioId());
        assertFalse(n.isLeida());
    }

    @Test
    void crearRechazaMensajeVacio() {
        UUID destinatario = UUID.randomUUID();

        assertThrows(ReglaDominioException.class,
                () -> Notificacion.crear(destinatario, null, TipoNotificacion.NUEVA_VENTA, "  "));
    }
}
