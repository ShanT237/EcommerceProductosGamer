package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PuntosTest {

    @Test
    void deberiaCrearPuntosConValorValido() {
        Puntos puntos = new Puntos(10);

        assertEquals(10, puntos.valor());
    }

    @Test
    void deberiaPermitirValorCero() {
        Puntos puntos = new Puntos(0);

        assertEquals(0, puntos.valor());
    }

    @Test
    void deberiaLanzarExcepcionSiValorEsNegativo() {
        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> new Puntos(-1)
        );
        assertEquals("Los puntos no pueden ser negativos", excepcion.getMessage());
    }

    @Test
    void ceroDebeRetornarPuntosConValorCero() {
        assertEquals(0, Puntos.cero().valor());
    }

    @Test
    void sumarDebeRetornarNuevoPuntosConLaSuma() {
        Puntos p1 = new Puntos(5);
        Puntos p2 = new Puntos(3);

        Puntos resultado = p1.sumar(p2);

        assertEquals(8, resultado.valor());
    }

    @Test
    void sumarNoDebeModificarLosPuntosOriginales() {
        Puntos p1 = new Puntos(5);
        Puntos p2 = new Puntos(3);

        p1.sumar(p2);

        assertEquals(5, p1.valor());
        assertEquals(3, p2.valor());
    }

    @Test
    void sumarConCeroDebeRetornarMismoValor() {
        Puntos p1 = new Puntos(7);

        Puntos resultado = p1.sumar(Puntos.cero());

        assertEquals(7, resultado.valor());
    }

    @Test
    void dosPuntosConMismoValorDebenSerIguales() {
        assertEquals(new Puntos(4), new Puntos(4));
        assertEquals(new Puntos(4).hashCode(), new Puntos(4).hashCode());
    }

    @Test
    void dosPuntosConDistintoValorNoDebenSerIguales() {
        assertNotEquals(new Puntos(4), new Puntos(5));
    }
}