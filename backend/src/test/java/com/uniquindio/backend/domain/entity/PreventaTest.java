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
        Preventa preventa = Preventa.programar(id, productoId, fecha, "Edición especial navideña");

        assertEquals(id, preventa.getId());
        assertEquals(productoId, preventa.getProductoId());
        assertEquals(fecha, preventa.getFechaLanzamiento());
        assertEquals("Edición especial navideña", preventa.getDescripcion());

        assertThrows(ReglaDominioException.class,
                () -> Preventa.programar(id, productoId, fecha, ""));
        assertThrows(ReglaDominioException.class,
                () -> Preventa.programar(id, productoId, fecha, "   "));
        assertThrows(NullPointerException.class,
                () -> Preventa.programar(id, productoId, null, "Descripción válida"));
        assertThrows(NullPointerException.class,
                () -> Preventa.programar(id, null, fecha, "Descripción válida"));
    }

    @Test
    @DisplayName("programar con hoy exige fecha de lanzamiento estrictamente futura")
    void programarExigeFechaFutura() {
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        UUID id = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        Preventa preventa = Preventa.programar(id, productoId, hoy.plusDays(1), "Mañana", hoy);
        assertEquals(hoy.plusDays(1), preventa.getFechaLanzamiento());

        assertThrows(ReglaDominioException.class,
                () -> Preventa.programar(id, productoId, hoy, "Hoy", hoy));
        assertThrows(ReglaDominioException.class,
                () -> Preventa.programar(id, productoId, hoy.minusDays(1), "Ayer", hoy));
    }

    @Test
    @DisplayName("El producto de preventa está disponible en la fecha de lanzamiento o después")
    void disponibleEnFechaODespues() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 10, 15);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Lanzamiento octubre");

        assertTrue(preventa.estaDisponible(LocalDate.of(2026, 10, 15)));
        assertTrue(preventa.estaDisponible(LocalDate.of(2026, 10, 16)));
        assertTrue(preventa.estaDisponible(LocalDate.of(2027, 1, 1)));
    }

    @Test
    @DisplayName("El producto de preventa NO está disponible antes de la fecha de lanzamiento")
    void noDisponibleAntesDeFecha() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 10, 15);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Lanzamiento octubre");

        assertFalse(preventa.estaDisponible(LocalDate.of(2026, 10, 14)));
        assertFalse(preventa.estaDisponible(LocalDate.of(2026, 1, 1)));
    }

    @Test
    @DisplayName("validarAcceso lanza excepción si se intenta acceder antes de la fecha de lanzamiento")
    void validarAccesoAntesDeFecha() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 12, 1);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Producto de fin de año");

        assertThrows(ReglaDominioException.class,
                () -> preventa.validarAcceso(LocalDate.of(2026, 11, 30)));
    }

    @Test
    @DisplayName("validarAcceso no lanza excepción en la fecha de lanzamiento o después")
    void validarAccesoEnFechaODespues() {
        LocalDate fechaLanzamiento = LocalDate.of(2026, 12, 1);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                fechaLanzamiento, "Producto de fin de año");

        assertDoesNotThrow(() -> preventa.validarAcceso(LocalDate.of(2026, 12, 1)));
        assertDoesNotThrow(() -> preventa.validarAcceso(LocalDate.of(2026, 12, 2)));
    }

    @Test
    @DisplayName("La identidad de la preventa depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Preventa preventa1 = Preventa.programar(id, UUID.randomUUID(),
                LocalDate.of(2026, 6, 1), "Preventa A");
        Preventa preventa2 = Preventa.programar(id, UUID.randomUUID(),
                LocalDate.of(2027, 1, 1), "Preventa B");
        Preventa preventa3 = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                LocalDate.of(2026, 6, 1), "Preventa A");

        assertEquals(preventa1, preventa2);
        assertEquals(preventa1.hashCode(), preventa2.hashCode());
        assertNotEquals(preventa1, preventa3);
    }

    @Test
    @DisplayName("reprogramar cambia fecha y descripción de una preventa futura")
    void reprogramarCambiaFechaYDescripcion() {
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(),
                hoy.plusDays(5), "Original", hoy);

        preventa.reprogramar(hoy.plusDays(30), "Nueva descripción", hoy);

        assertEquals(hoy.plusDays(30), preventa.getFechaLanzamiento());
        assertEquals("Nueva descripción", preventa.getDescripcion());
    }

    @Test
    @DisplayName("reprogramar exige fecha estrictamente futura y descripción no vacía, sin alterar la preventa")
    void reprogramarValidaDatosYNoAlteraLaPreventa() {
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        LocalDate original = hoy.plusDays(5);
        Preventa preventa = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(), original, "Original", hoy);

        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(hoy, "Hoy", hoy));
        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(hoy.minusDays(1), "Ayer", hoy));
        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(null, "Sin fecha", hoy));
        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(hoy.plusDays(9), "", hoy));
        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(hoy.plusDays(9), "   ", hoy));
        assertThrows(ReglaDominioException.class, () -> preventa.reprogramar(hoy.plusDays(9), null, hoy));

        assertEquals(original, preventa.getFechaLanzamiento());
        assertEquals("Original", preventa.getDescripcion());
    }

    @Test
    @DisplayName("Una preventa ya lanzada no se puede reprogramar ni cancelar")
    void preventaLanzadaNoSePuedeModificar() {
        LocalDate hoy = LocalDate.of(2026, 10, 20);
        Preventa enLaFecha = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(), hoy, "Hoy");
        Preventa yaPasada = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(), hoy.minusDays(3), "Pasada");

        assertThrows(ReglaDominioException.class,
                () -> enLaFecha.reprogramar(hoy.plusDays(10), "Nueva", hoy));
        assertThrows(ReglaDominioException.class,
                () -> yaPasada.reprogramar(hoy.plusDays(10), "Nueva", hoy));
        assertThrows(ReglaDominioException.class, () -> enLaFecha.asegurarNoLanzada(hoy, "cancelar"));
        assertThrows(ReglaDominioException.class, () -> yaPasada.asegurarNoLanzada(hoy, "cancelar"));

        Preventa futura = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(), hoy.plusDays(1), "Mañana");
        assertDoesNotThrow(() -> futura.asegurarNoLanzada(hoy, "cancelar"));
    }
}
