package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductoTest {

    private Producto crearProductoValido() {
        return new Producto(UUID.randomUUID(), "Mouse Gamer", "Mouse con RGB",
                59.99, 10, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    @DisplayName("Crea un producto válido con todos sus atributos, y falla con datos inválidos")
    void creaProductoValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        Producto producto = new Producto(id, "Teclado Mecánico", "Switches Cherry MX",
                129.99, 5, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertEquals(id, producto.getId());
        assertEquals("Teclado Mecánico", producto.getNombre());
        assertEquals("Switches Cherry MX", producto.getDescripcion());
        assertEquals(129.99, producto.getPrecio());
        assertEquals(5, producto.getStock());
        assertEquals(Gama.ALTA, producto.getGama());
        assertEquals(Exclusividad.EXCLUSIVO, producto.getExclusividad());
        assertFalse(producto.isEliminado());
        assertTrue(producto.esExclusivo());

        assertThrows(ReglaDominioException.class,
                () -> new Producto(id, "", "Desc", 10, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> new Producto(id, "Nombre", "", 10, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> new Producto(id, "Nombre", "Desc", 0, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> new Producto(id, "Nombre", "Desc", -1, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> new Producto(id, "Nombre", "Desc", 10, -1, Gama.BAJA, Exclusividad.NORMAL));
    }

    @Test
    @DisplayName("Permite crear un producto con stock cero (agotado)")
    void creaProductoConStockCero() {
        Producto producto = new Producto(UUID.randomUUID(), "Edición Limitada",
                "Sin stock", 200, 0, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertEquals(0, producto.getStock());
    }

    @Test
    @DisplayName("reducirStock disminuye correctamente y rechaza stock negativo (Regla 1)")
    void reducirStock() {
        Producto producto = crearProductoValido();

        producto.reducirStock(3);
        assertEquals(7, producto.getStock());

        producto.reducirStock(7);
        assertEquals(0, producto.getStock());

        assertThrows(ReglaDominioException.class, () -> producto.reducirStock(1));
    }

    @Test
    @DisplayName("reducirStock rechaza cantidades no positivas")
    void reducirStockCantidadInvalida() {
        Producto producto = crearProductoValido();

        assertThrows(ReglaDominioException.class, () -> producto.reducirStock(0));
        assertThrows(ReglaDominioException.class, () -> producto.reducirStock(-1));
    }

    @Test
    @DisplayName("aumentarStock incrementa correctamente y rechaza cantidades no positivas")
    void aumentarStock() {
        Producto producto = crearProductoValido();

        producto.aumentarStock(5);
        assertEquals(15, producto.getStock());

        assertThrows(ReglaDominioException.class, () -> producto.aumentarStock(0));
        assertThrows(ReglaDominioException.class, () -> producto.aumentarStock(-3));
    }

    @Test
    @DisplayName("Elimina lógicamente un producto sin compras activas (Regla 3)")
    void eliminarSinComprasActivas() {
        Producto producto = crearProductoValido();

        producto.eliminar(false);

        assertTrue(producto.isEliminado());
    }

    @Test
    @DisplayName("Rechaza eliminar un producto con compras activas (Regla 3)")
    void rechazaEliminarConComprasActivas() {
        Producto producto = crearProductoValido();

        assertThrows(ReglaDominioException.class, () -> producto.eliminar(true));
        assertFalse(producto.isEliminado());
    }

    @Test
    @DisplayName("esExclusivo delega correctamente al value object Exclusividad")
    void esExclusivo() {
        Producto normal = new Producto(UUID.randomUUID(), "Mouse", "Desc",
                30, 10, Gama.BAJA, Exclusividad.NORMAL);
        Producto exclusivo = new Producto(UUID.randomUUID(), "Mouse Pro", "Desc",
                100, 2, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertFalse(normal.esExclusivo());
        assertTrue(exclusivo.esExclusivo());
    }

    @Test
    @DisplayName("La identidad del producto depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Producto producto1 = new Producto(id, "Nombre A", "Desc A",
                10, 5, Gama.BAJA, Exclusividad.NORMAL);
        Producto producto2 = new Producto(id, "Nombre B", "Desc B",
                20, 10, Gama.ALTA, Exclusividad.EXCLUSIVO);
        Producto producto3 = new Producto(UUID.randomUUID(), "Nombre A", "Desc A",
                10, 5, Gama.BAJA, Exclusividad.NORMAL);

        assertEquals(producto1, producto2);
        assertEquals(producto1.hashCode(), producto2.hashCode());
        assertNotEquals(producto1, producto3);
    }
}
