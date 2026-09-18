package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegaloTest {

    @Test
    @DisplayName("Crea un regalo válido no entregado, y falla con datos inválidos")
    void creaRegaloValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        Regalo regalo = new Regalo(id, "Mouse gamer edición especial", 100);

        assertEquals(id, regalo.getId());
        assertEquals("Mouse gamer edición especial", regalo.getDescripcion());
        assertEquals(100, regalo.getPuntosRequeridos());
        assertFalse(regalo.isEntregado());
        assertNull(regalo.getUsuarioId());

        assertThrows(ReglaDominioException.class,
                () -> new Regalo(id, "", 100));
        assertThrows(ReglaDominioException.class,
                () -> new Regalo(id, "   ", 100));
        assertThrows(ReglaDominioException.class,
                () -> new Regalo(id, "Regalo válido", 0));
        assertThrows(ReglaDominioException.class,
                () -> new Regalo(id, "Regalo válido", -5));
    }

    @Test
    @DisplayName("Entrega el regalo cuando el usuario cumple la meta de puntos")
    void entregarConMetaCumplida() {
        UUID regaloId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Regalo regalo = new Regalo(regaloId, "Teclado mecánico", 50);

        regalo.entregar(usuarioId, 50);

        assertTrue(regalo.isEntregado());
        assertEquals(usuarioId, regalo.getUsuarioId());
    }

    @Test
    @DisplayName("Entrega el regalo cuando el usuario supera la meta de puntos")
    void entregarConPuntosSuperados() {
        UUID regaloId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        Regalo regalo = new Regalo(regaloId, "Audífonos gamer", 30);

        regalo.entregar(usuarioId, 100);

        assertTrue(regalo.isEntregado());
        assertEquals(usuarioId, regalo.getUsuarioId());
    }

    @Test
    @DisplayName("Rechaza entregar el regalo si el usuario no cumple la meta de puntos")
    void rechazaEntregarSinMetaCumplida() {
        Regalo regalo = new Regalo(UUID.randomUUID(), "Mousepad XL", 100);
        UUID usuarioId = UUID.randomUUID();

        assertThrows(ReglaDominioException.class,
                () -> regalo.entregar(usuarioId, 99));
        assertFalse(regalo.isEntregado());
        assertNull(regalo.getUsuarioId());
    }

    @Test
    @DisplayName("Rechaza entregar un regalo que ya fue entregado")
    void rechazaEntregarDosVeces() {
        Regalo regalo = new Regalo(UUID.randomUUID(), "Silla gamer", 50);
        UUID usuario1 = UUID.randomUUID();
        UUID usuario2 = UUID.randomUUID();

        regalo.entregar(usuario1, 50);

        assertThrows(ReglaDominioException.class,
                () -> regalo.entregar(usuario2, 200));
    }

    @Test
    @DisplayName("La identidad del regalo depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Regalo regalo1 = new Regalo(id, "Regalo A", 10);
        Regalo regalo2 = new Regalo(id, "Regalo B", 20);
        Regalo regalo3 = new Regalo(UUID.randomUUID(), "Regalo A", 10);

        assertEquals(regalo1, regalo2);
        assertEquals(regalo1.hashCode(), regalo2.hashCode());
        assertNotEquals(regalo1, regalo3);
    }
}
