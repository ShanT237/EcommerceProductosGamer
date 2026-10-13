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
        return Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse Gamer", "Mouse con RGB",
                59.99, 10, Gama.MEDIA, Exclusividad.NORMAL);
    }

    @Test
    @DisplayName("Crea un producto válido con todos sus atributos, y falla con datos inválidos")
    void creaProductoValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(id, vendedorId, "Teclado Mecánico", "Switches Cherry MX",
                129.99, 5, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertEquals(id, producto.getId());
        assertEquals(vendedorId, producto.getVendedorId());
        assertEquals("Teclado Mecánico", producto.getNombre());
        assertEquals("Switches Cherry MX", producto.getDescripcion());
        assertEquals(129.99, producto.getPrecio());
        assertEquals(5, producto.getStock());
        assertEquals(Gama.ALTA, producto.getGama());
        assertEquals(Exclusividad.EXCLUSIVO, producto.getExclusividad());
        assertFalse(producto.isEliminado());
        assertTrue(producto.esExclusivo());

        assertThrows(ReglaDominioException.class,
                () -> Producto.publicar(id, vendedorId, "", "Desc", 10, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> Producto.publicar(id, vendedorId, "Nombre", "", 10, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> Producto.publicar(id, vendedorId, "Nombre", "Desc", 0, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> Producto.publicar(id, vendedorId, "Nombre", "Desc", -1, 5, Gama.BAJA, Exclusividad.NORMAL));
        assertThrows(ReglaDominioException.class,
                () -> Producto.publicar(id, vendedorId, "Nombre", "Desc", 10, -1, Gama.BAJA, Exclusividad.NORMAL));
    }

    @Test
    @DisplayName("Rechaza crear un producto sin vendedor (todo producto pertenece a un vendedor)")
    void rechazaProductoSinVendedor() {
        assertThrows(NullPointerException.class,
                () -> Producto.publicar(UUID.randomUUID(), null, "Mouse", "Desc",
                        30, 10, Gama.BAJA, Exclusividad.NORMAL));
    }

    @Test
    @DisplayName("Permite crear un producto con stock cero (agotado)")
    void creaProductoConStockCero() {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Edición Limitada",
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
    @DisplayName("aplicarDescuento reduce el precio y rechaza porcentajes inválidos")
    void aplicarDescuento() {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Desc",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);

        producto.aplicarDescuento(10);
        assertEquals(90.0, producto.getPrecio(), 0.001);

        assertThrows(ReglaDominioException.class, () -> producto.aplicarDescuento(0));
        assertThrows(ReglaDominioException.class, () -> producto.aplicarDescuento(100));
    }

    @Test
    @DisplayName("Elimina lógicamente un producto sin compras activas (Regla 3)")
    void eliminarSinComprasActivas() {
        Producto producto = crearProductoValido();

        producto.eliminar(false);

        assertTrue(producto.isEliminado());
    }

    @Test
    @DisplayName("Rechaza eliminar un producto que ya está eliminado")
    void rechazaEliminarDosVeces() {
        Producto producto = crearProductoValido();
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class, () -> producto.eliminar(false));
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
        Producto normal = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Desc",
                30, 10, Gama.BAJA, Exclusividad.NORMAL);
        Producto exclusivo = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse Pro", "Desc",
                100, 2, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertFalse(normal.esExclusivo());
        assertTrue(exclusivo.esExclusivo());
    }

    @Test
    @DisplayName("La identidad del producto depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Producto producto1 = Producto.publicar(id, UUID.randomUUID(), "Nombre A", "Desc A",
                10, 5, Gama.BAJA, Exclusividad.NORMAL);
        Producto producto2 = Producto.publicar(id, UUID.randomUUID(), "Nombre B", "Desc B",
                20, 10, Gama.ALTA, Exclusividad.EXCLUSIVO);
        Producto producto3 = Producto.publicar(UUID.randomUUID(), producto1.getVendedorId(), "Nombre A", "Desc A",
                10, 5, Gama.BAJA, Exclusividad.NORMAL);

        assertEquals(producto1, producto2);
        assertEquals(producto1.hashCode(), producto2.hashCode());
        assertNotEquals(producto1, producto3);
    }

    @Test
    @DisplayName("Un exclusivo no se puede comprar si el usuario ya lo tiene (Regla 9)")
    void rechazaExclusivoYaComprado() {
        Producto exclusivo = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse Pro",
                "Desc", 100, 5, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertThrows(ReglaDominioException.class, () -> exclusivo.validarCompraExclusiva(true, 1));
        assertDoesNotThrow(() -> exclusivo.validarCompraExclusiva(false, 1));
    }

    @Test
    @DisplayName("Un exclusivo solo se compra de a una unidad (Regla 9)")
    void rechazaExclusivoConCantidadMayorAUno() {
        Producto exclusivo = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse Pro",
                "Desc", 100, 5, Gama.ALTA, Exclusividad.EXCLUSIVO);

        assertThrows(ReglaDominioException.class, () -> exclusivo.validarCompraExclusiva(false, 2));
    }

    @Test
    @DisplayName("Un producto normal no tiene restricción de compra ni de cantidad")
    void productoNormalSinRestriccion() {
        Producto normal = crearProductoValido();

        assertDoesNotThrow(() -> normal.validarCompraExclusiva(true, 3));
    }

    @Test
    @DisplayName("La cantidad inválida se reporta antes que la regla de exclusividad")
    void validaCantidadAntesQueExclusividad() {
        Producto exclusivo = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse Pro",
                "Desc", 100, 5, Gama.ALTA, Exclusividad.EXCLUSIVO);
        Producto normal = crearProductoValido();

        ReglaDominioException e1 = assertThrows(ReglaDominioException.class,
                () -> exclusivo.validarCompraExclusiva(true, 0));
        assertEquals("La cantidad debe ser positiva", e1.getMessage());

        ReglaDominioException e2 = assertThrows(ReglaDominioException.class,
                () -> exclusivo.validarCompraExclusiva(true, 2));
        assertTrue(e2.getMessage().contains("una unidad"));

        assertThrows(ReglaDominioException.class, () -> normal.validarCompraExclusiva(false, 0));
    }
}