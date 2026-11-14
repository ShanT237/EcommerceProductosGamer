package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductoDescuentoTest {

    private Producto producto() {
        return Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    void quitarDescuentoRestauraElPrecioOriginal() {
        Producto p = producto();
        p.aplicarDescuento(20);

        p.quitarDescuento();

        assertEquals(100.0, p.getPrecio(), 0.001);
        assertNull(p.getPrecioOriginal());
        assertFalse(p.tieneDescuento());
    }

    @Test
    void quitarDescuentoSinDescuentoActivoRechaza() {
        Producto p = producto();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, p::quitarDescuento);

        assertEquals("El producto no tiene un descuento activo", ex.getMessage());
        assertEquals(100.0, p.getPrecio(), 0.001);
    }

    @Test
    void despuesDeQuitarSePuedeAplicarOtroDescuento() {
        Producto p = producto();
        p.aplicarDescuento(20);
        p.quitarDescuento();

        p.aplicarDescuento(10);

        assertEquals(90.0, p.getPrecio(), 0.001);
        assertEquals(100.0, p.getPrecioOriginal());
    }

    @Test
    void elPorcentajeSeCalculaDesdeElPrecioOriginal() {
        Producto p = producto();
        assertNull(p.getPorcentajeDescuento());

        p.aplicarDescuento(20);

        assertTrue(p.tieneDescuento());
        assertEquals(20.0, p.getPorcentajeDescuento(), 0.001);
    }

    @Test
    void cambiarPrecioConDescuentoActivoRechazaYNoCambiaNada() {
        Producto p = producto();
        p.aplicarDescuento(20);

        assertThrows(ReglaDominioException.class, () -> p.cambiarPrecio(150));

        assertEquals(80.0, p.getPrecio(), 0.001);
        assertEquals(100.0, p.getPrecioOriginal());
    }

    @Test
    void cambiarPrecioSinDescuentoSigueFuncionando() {
        Producto p = producto();

        p.cambiarPrecio(150);

        assertEquals(150.0, p.getPrecio(), 0.001);
    }

    @Test
    void quitarElDescuentoYLuegoCambiarElPrecioFunciona() {
        Producto p = producto();
        p.aplicarDescuento(20);
        p.quitarDescuento();

        p.cambiarPrecio(150);

        assertEquals(150.0, p.getPrecio(), 0.001);
    }
}
