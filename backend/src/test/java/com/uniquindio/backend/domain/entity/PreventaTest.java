package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PreventaTest {

    @Test
    @DisplayName("Crea una preventa válida, y falla con datos inválidos")
    void creaPreventaValidaYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        LocalDate fecha = LocalDate.of(2026, 12, 25);
        Preventa preventa = new Preventa(id, productoId, fecha, "Edición especial navideña");

        assertEquals(id, preventa.getId());
        assertEquals(productoId, preventa.getProductoId());
        assertEquals(fecha, preventa.getFechaLanzamiento());
        assertEquals("Edición especial navideña", preventa.getDescripcion());

        assertThrows(ReglaDominioException.class,
                () -> new Preventa(id, productoId, fecha, ""));
        assertThrows(ReglaDominioException.class,
                () -> new Preventa(id, productoId, fecha, "   "));
        assertThrows(NullPointerException.class,
                () -> new Preventa(id, productoId, null, "Descripción válida"));
        assertThrows(NullPointerException.class,
                () -> new Preventa(id, null, fecha, "Descripción válida"));
    }

    @Test
    @DisplayName("El producto de preventa está disponible en la fecha de lanzamiento o después")
    void disponibleEnFechaODespues() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 10, 15);
        Preventa preventa = new Preventa(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Lanzamiento octubre");

        assertTrue(preventa.estaDisponible(LocalDate.of(2026, 10, 15)));
        assertTrue(preventa.estaDisponible(LocalDate.of(2026, 10, 16)));
        assertTrue(preventa.estaDisponible(LocalDate.of(2027, 1, 1)));
    }

    @Test
    @DisplayName("El producto de preventa NO está disponible antes de la fecha de lanzamiento")
    void noDisponibleAntesDeFecha() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 10, 15);
        Preventa preventa = new Preventa(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Lanzamiento octubre");

        assertFalse(preventa.estaDisponible(LocalDate.of(2026, 10, 14)));
        assertFalse(preventa.estaDisponible(LocalDate.of(2026, 1, 1)));
    }

    @Test
    @DisplayName("validarAcceso lanza excepción si se intenta acceder antes de la fecha de lanzamiento")
    void validarAccesoAntesDeFecha() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 12, 1);
        Preventa preventa = new Preventa(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Producto de fin de año");

        assertThrows(ReglaDominioException.class,
                () -> preventa.validarAcceso(LocalDate.of(2026, 11, 30)));
    }

    @Test
    @DisplayName("validarAcceso no lanza excepción en la fecha de lanzamiento o después")
    void validarAccesoEnFechaODespues() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 12, 1);
        Preventa preventa = new Preventa(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Producto de fin de año");

        assertDoesNotThrow(() -> preventa.validarAcceso(LocalDate.of(2026, 12, 1)));
        assertDoesNotThrow(() -> preventa.validarAcceso(LocalDate.of(2026, 12, 2)));
    }

    @Test
    @DisplayName("La identidad de la preventa depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Preventa preventa1 = new Preventa(id, UUID.randomUUID(),
                LocalDate.of(2026, 6, 1), "Preventa A");
        Preventa preventa2 = new Preventa(id, UUID.randomUUID(),
                LocalDate.of(2027, 1, 1), "Preventa B");
        Preventa preventa3 = new Preventa(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 6, 1), "Preventa A");

        assertEquals(preventa1, preventa2);
        assertEquals(preventa1.hashCode(), preventa2.hashCode());
        assertNotEquals(preventa1, preventa3);
    }
}
