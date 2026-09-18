package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CalificacionTest {

    @Test
    void crearCalificacionValida_debeAceptarValoresEntre1y5() {
        for (int i = 1; i <= 5; i++) {
            Calificacion calificacion = new Calificacion(i);
            assertEquals(i, calificacion.estrellas());
        }
    }

    @Test
    void calificacionMenorA1_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () -> new Calificacion(0));
    }

    @Test
    void calificacionMayorA5_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () -> new Calificacion(6));
    }

    @Test
    void calificacionNegativa_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () -> new Calificacion(-3));
    }

    @Test
    void esPositiva_debeSerTrueParaCuatroOCinco() {
        assertTrue(new Calificacion(4).esPositiva());
        assertTrue(new Calificacion(5).esPositiva());
        assertFalse(new Calificacion(3).esPositiva());
    }

    @Test
    void esNegativa_debeSerTrueParaUnoODos() {
        assertTrue(new Calificacion(1).esNegativa());
        assertTrue(new Calificacion(2).esNegativa());
        assertFalse(new Calificacion(3).esNegativa());
    }

    @Test
    void dosCalificacionesConMismoValor_debenSerIguales() {
        assertEquals(new Calificacion(5), new Calificacion(5));
    }

    @Test
    void dosCalificacionesConDistintoValor_noDebenSerIguales() {
        assertNotEquals(new Calificacion(5), new Calificacion(3));
    }
}