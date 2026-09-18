package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResenaTest {

    private Resena crearResenaValida() {
        return Resena.crear(
                "resena-1", "compra-1", "producto-1", "usuario-1",
                true, new Calificacion(5), "Excelente mouse, muy cómodo");
    }

    @Test
    void crearResenaValida_debeCrearseCorrectamente() {
        Resena resena = crearResenaValida();

        assertEquals("resena-1", resena.getId());
        assertEquals("compra-1", resena.getIdCompra());
        assertEquals(5, resena.getCalificacion().estrellas());
        assertFalse(resena.tieneRespuesta());
        assertNotNull(resena.getFechaCreacion());
    }

    @Test
    void crearResenaSinCompraCompletada_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () ->
                Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                        false, new Calificacion(4), "Buen producto"));
    }

    @Test
    void crearResenaSinCalificacion_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () ->
                Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                        true, null, "Buen producto"));
    }

    @Test
    void crearResenaConComentarioNulo_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () ->
                Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                        true, new Calificacion(4), null));
    }

    @Test
    void crearResenaConComentarioVacio_debeLanzarExcepcion() {
        assertThrows(ReglaDominioException.class, () ->
                Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                        true, new Calificacion(4), "   "));
    }

    @Test
    void crearResenaConComentarioMuyLargo_debeLanzarExcepcion() {
        String comentarioLargo = "a".repeat(501);
        assertThrows(ReglaDominioException.class, () ->
                Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                        true, new Calificacion(4), comentarioLargo));
    }

    @Test
    void crearResenaConComentarioDeExactamente500Caracteres_debeSerValida() {
        String comentarioLimite = "a".repeat(500);
        Resena resena = Resena.crear("resena-1", "compra-1", "producto-1", "usuario-1",
                true, new Calificacion(4), comentarioLimite);
        assertEquals(500, resena.getComentario().length());
    }

    @Test
    void responderResena_debeGuardarRespuestaDelVendedor() {
        Resena resena = crearResenaValida();
        resena.responder("¡Gracias por tu compra!");

        assertTrue(resena.tieneRespuesta());
        assertEquals("¡Gracias por tu compra!", resena.getRespuestaVendedor());
        assertNotNull(resena.getFechaRespuesta());
    }

    @Test
    void responderResenaConRespuestaVacia_debeLanzarExcepcion() {
        Resena resena = crearResenaValida();
        assertThrows(ReglaDominioException.class, () -> resena.responder(""));
    }

    @Test
    void responderResenaDosVeces_debeLanzarExcepcion() {
        Resena resena = crearResenaValida();
        resena.responder("Primera respuesta");

        assertThrows(ReglaDominioException.class, () -> resena.responder("Segunda respuesta"));
    }

    @Test
    void dosResenasConMismoId_debenSerIguales() {
        Resena resena1 = crearResenaValida();
        Resena resena2 = Resena.crear("resena-1", "compra-2", "producto-2", "usuario-2",
                true, new Calificacion(1), "Otra reseña, mismo id");

        assertEquals(resena1, resena2);
        assertEquals(resena1.hashCode(), resena2.hashCode());
    }

    @Test
    void dosResenasConDistintoId_noDebenSerIguales() {
        Resena resena1 = crearResenaValida();
        Resena resena2 = Resena.crear("resena-2", "compra-1", "producto-1", "usuario-1",
                true, new Calificacion(5), "Excelente mouse, muy cómodo");

        assertNotEquals(resena1, resena2);
    }
}