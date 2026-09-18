package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompraTest {

    private Compra crearCompraValida() {
        return new Compra(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                2, LocalDate.of(2026, 9, 17));
    }

    @Test
    @DisplayName("Crea una compra válida sin reseña ni puntos, y falla con datos inválidos")
    void creaCompraValidaYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 9, 17);

        Compra compra = new Compra(id, usuarioId, productoId, 3, fecha);

        assertEquals(id, compra.getId());
        assertEquals(usuarioId, compra.getUsuarioId());
        assertEquals(productoId, compra.getProductoId());
        assertEquals(3, compra.getCantidad());
        assertEquals(fecha, compra.getFecha());
        assertFalse(compra.isResenada());
        assertFalse(compra.isPuntosAsignados());

        assertThrows(ReglaDominioException.class,
                () -> new Compra(id, usuarioId, productoId, 0, fecha));
        assertThrows(ReglaDominioException.class,
                () -> new Compra(id, usuarioId, productoId, -1, fecha));
        assertThrows(NullPointerException.class,
                () -> new Compra(id, usuarioId, productoId, 1, null));
    }

    @Test
    @DisplayName("Registra una reseña correctamente y rechaza reseñar dos veces (Regla 2)")
    void registrarResena() {
        Compra compra = crearCompraValida();

        compra.registrarResena();
        assertTrue(compra.isResenada());

        assertThrows(ReglaDominioException.class, compra::registrarResena);
    }

    @Test
    @DisplayName("Asigna puntos solo después de la reseña (Regla 6)")
    void asignarPuntosDespuesDeResena() {
        Compra compra = crearCompraValida();

        compra.registrarResena();
        int puntos = compra.asignarPuntos(10);

        assertEquals(10, puntos);
        assertTrue(compra.isPuntosAsignados());
    }

    @Test
    @DisplayName("Rechaza asignar puntos sin haber reseñado (Regla 6)")
    void rechazaAsignarPuntosSinResena() {
        Compra compra = crearCompraValida();

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(10));
        assertFalse(compra.isPuntosAsignados());
    }

    @Test
    @DisplayName("Rechaza asignar puntos dos veces a la misma compra")
    void rechazaAsignarPuntosDosVeces() {
        Compra compra = crearCompraValida();
        compra.registrarResena();
        compra.asignarPuntos(10);

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(10));
    }

    @Test
    @DisplayName("Rechaza asignar puntos con valor no positivo")
    void rechazaAsignarPuntosNoPositivos() {
        Compra compra = crearCompraValida();
        compra.registrarResena();

        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(0));
        assertThrows(ReglaDominioException.class, () -> compra.asignarPuntos(-5));
    }

    @Test
    @DisplayName("La identidad de la compra depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 9, 17);
        Compra compra1 = new Compra(id, UUID.randomUUID(), UUID.randomUUID(), 1, fecha);
        Compra compra2 = new Compra(id, UUID.randomUUID(), UUID.randomUUID(), 5, fecha);
        Compra compra3 = new Compra(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), 1, fecha);

        assertEquals(compra1, compra2);
        assertEquals(compra1.hashCode(), compra2.hashCode());
        assertNotEquals(compra1, compra3);
    }
}
