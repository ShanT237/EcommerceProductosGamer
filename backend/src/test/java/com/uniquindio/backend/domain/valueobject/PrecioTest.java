package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PrecioTest {

    @Test
    void deberiaCrearPrecioConValorValido() {
        Precio precio = new Precio(1000);

        assertEquals(1000, precio.valor());
    }

    @Test
    void deberiaLanzarExcepcionSiValorEsCero() {
        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> new Precio(0)
        );
        assertEquals("El precio debe ser positivo", excepcion.getMessage());
    }

    @Test
    void deberiaLanzarExcepcionSiValorEsNegativo() {
        assertThrows(ReglaDominioException.class, () -> new Precio(-500));
    }

    @Test
    void aplicarDescuentoDebeRetornarNuevoPrecioConLaReduccion() {
        Precio precio = new Precio(1000);

        Precio resultado = precio.aplicarDescuento(10);

        assertEquals(900, resultado.valor());
    }

    @Test
    void aplicarDescuentoNoDebeModificarElPrecioOriginal() {
        Precio precio = new Precio(1000);

        precio.aplicarDescuento(20);

        assertEquals(1000, precio.valor());
    }

    @Test
    void aplicarDescuentoConPorcentajeNegativoDebeLanzarExcepcion() {
        Precio precio = new Precio(1000);

        assertThrows(ReglaDominioException.class, () -> precio.aplicarDescuento(-5));
    }

    @Test
    void aplicarDescuentoDelCienPorCientoDebeLanzarExcepcion() {
        Precio precio = new Precio(1000);

        assertThrows(ReglaDominioException.class, () -> precio.aplicarDescuento(100));
    }

    @Test
    void sumarDebeRetornarNuevoPrecioConLaSuma() {
        Precio p1 = new Precio(500);
        Precio p2 = new Precio(300);

        Precio resultado = p1.sumar(p2);

        assertEquals(800, resultado.valor());
    }

    @Test
    void dosPreciosConMismoValorDebenSerIguales() {
        assertEquals(new Precio(200), new Precio(200));
        assertEquals(new Precio(200).hashCode(), new Precio(200).hashCode());
    }

    @Test
    void dosPreciosConDistintoValorNoDebenSerIguales() {
        assertNotEquals(new Precio(200), new Precio(300));
    }
}