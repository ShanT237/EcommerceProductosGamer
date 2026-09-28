package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UnidadesTest {

    @Test
    void deberiaCrearUnidadesConCantidadValida() {
        Unidades unidades = new Unidades(10);

        assertEquals(10, unidades.cantidad());
    }

    @Test
    void deberiaLanzarExcepcionSiCantidadEsCero() {
        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> new Unidades(0)
        );
        assertEquals("Las unidades deben ser mayores a cero", excepcion.getMessage());
    }

    @Test
    void deberiaLanzarExcepcionSiCantidadEsNegativa() {
        assertThrows(ReglaDominioException.class, () -> new Unidades(-5));
    }

    @Test
    void restarDebeRetornarNuevasUnidadesConLaResta() {
        Unidades unidades = new Unidades(10);

        Unidades resultado = unidades.restar(4);

        assertEquals(6, resultado.cantidad());
    }

    @Test
    void restarNoDebeModificarLasUnidadesOriginales() {
        Unidades unidades = new Unidades(10);

        unidades.restar(4);

        assertEquals(10, unidades.cantidad());
    }

    @Test
    void restarHastaDejarEnCeroDebeLanzarExcepcion() {
        Unidades unidades = new Unidades(5);

        assertThrows(ReglaDominioException.class, () -> unidades.restar(5));
    }

    @Test
    void restarUnaCantidadMayorALaExistenteDebeLanzarExcepcion() {
        Unidades unidades = new Unidades(5);

        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> unidades.restar(10)
        );
        assertEquals("Las unidades deben ser mayores a cero", excepcion.getMessage());
    }

    @Test
    void dosUnidadesConMismaCantidadDebenSerIguales() {
        assertEquals(new Unidades(3), new Unidades(3));
        assertEquals(new Unidades(3).hashCode(), new Unidades(3).hashCode());
    }

    @Test
    void dosUnidadesConDistintaCantidadNoDebenSerIguales() {
        assertNotEquals(new Unidades(3), new Unidades(4));
    }
}