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
    void deberiaPermitirCantidadCero() {
        assertEquals(0, new Unidades(0).cantidad());
    }

    @Test
    void deberiaLanzarExcepcionSiCantidadEsNegativa() {
        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class, () -> new Unidades(-5));
        assertEquals("Las unidades no pueden ser negativas", excepcion.getMessage());
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
    void restarHastaDejarEnCeroDebeRetornarCero() {
        assertEquals(0, new Unidades(5).restar(5).cantidad());
    }

    @Test
    void restarUnaCantidadMayorALaExistenteDebeLanzarExcepcion() {
        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class, () -> new Unidades(5).restar(10));
        assertEquals("No hay unidades suficientes", excepcion.getMessage());
    }

    @Test
    void restarCantidadNoPositivaDebeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () -> new Unidades(5).restar(0));
        assertThrows(ReglaDominioException.class, () -> new Unidades(5).restar(-1));
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