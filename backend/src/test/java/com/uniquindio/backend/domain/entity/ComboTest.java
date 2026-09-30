package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ComboTest {

    @Test
    @DisplayName("Crea un combo válido con dos productos y valida campos y excepciones de validación")
    void creaComboValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        UUID producto1 = UUID.randomUUID();
        UUID producto2 = UUID.randomUUID();

        Combo combo = new Combo(id, vendedorId, "Combo Streamer", "Teclado + Mouse", 15.0, List.of(producto1, producto2));

        assertEquals(id, combo.getId());
        assertEquals(vendedorId, combo.getVendedorId());
        assertEquals("Combo Streamer", combo.getNombre());
        assertEquals("Teclado + Mouse", combo.getDescripcion());
        assertEquals(15.0, combo.getDescuento());
        assertEquals(2, combo.getProductos().size());
        assertTrue(combo.getProductos().contains(producto1));
        assertTrue(combo.getProductos().contains(producto2));

        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "", "Desc", 10.0, List.of(producto1, producto2)));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Nombre", "", 10.0, List.of(producto1, producto2)));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Nombre", "Desc", -5.0, List.of(producto1, producto2)));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Nombre", "Desc", 105.0, List.of(producto1, producto2)));
    }

    @Test
    @DisplayName("Rechaza crear un combo con menos de dos productos (Regla 5) o con productos duplicados")
    void rechazaComboInvalidoOConDuplicados() {
        UUID id = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        UUID producto1 = UUID.randomUUID();

        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Combo Inválido", "Solo un producto", 10.0, List.of(producto1)));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Combo Vacío", "Sin productos", 10.0, List.of()));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Combo Null", "Null productos", 10.0, null));
        assertThrows(ReglaDominioException.class,
                () -> new Combo(id, vendedorId, "Combo Duplicado", "Mismo producto dos veces", 10.0, List.of(producto1, producto1)));
    }

    @Test
    @DisplayName("Calcula el precio total del combo aplicando el descuento")
    void calculaPrecioTotalDelCombo() {
        UUID vendedorId = UUID.randomUUID();
        Producto p1 = new Producto(UUID.randomUUID(), vendedorId, "Teclado", "Teclado mecánico", 100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        Producto p2 = new Producto(UUID.randomUUID(), vendedorId, "Mouse", "Mouse óptico", 50.0, 10, Gama.MEDIA, Exclusividad.NORMAL);

        Combo combo = new Combo(UUID.randomUUID(), vendedorId, "Combo Gamer", "Teclado + Mouse", 20.0, List.of(p1.getId(), p2.getId()));

        double precioTotal = combo.calcularPrecioTotal(List.of(p1, p2));
        assertEquals(120.0, precioTotal, 0.001);
    }

    @Test
    @DisplayName("Permite agregar un nuevo producto y evita productos duplicados")
    void agregarProducto() {
        UUID vendedorId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        Combo combo = new Combo(UUID.randomUUID(), vendedorId, "Combo Gamer", "Setup completo", 20.0, List.of(p1, p2));

        combo.agregarProducto(p3);
        assertEquals(3, combo.getProductos().size());
        assertTrue(combo.getProductos().contains(p3));

        assertThrows(ReglaDominioException.class, () -> combo.agregarProducto(p1));
    }

    @Test
    @DisplayName("Permite quitar un producto si quedan al menos dos, y lo rechaza si quedan menos de dos (Regla 5)")
    void quitarProducto() {
        UUID vendedorId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        UUID p3 = UUID.randomUUID();
        Combo combo = new Combo(UUID.randomUUID(), vendedorId, "Combo Pro", "Setup gamer pro", 25.0, List.of(p1, p2, p3));

        combo.quitarProducto(p3);
        assertEquals(2, combo.getProductos().size());

        assertThrows(ReglaDominioException.class, () -> combo.quitarProducto(p2));
        assertThrows(ReglaDominioException.class, () -> combo.quitarProducto(UUID.randomUUID()));
    }

    @Test
    @DisplayName("La identidad del combo depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();

        Combo combo1 = new Combo(id, vendedorId, "Combo A", "Desc A", 10.0, List.of(p1, p2));
        Combo combo2 = new Combo(id, vendedorId, "Combo B", "Desc B", 20.0, List.of(p1, p2));
        Combo combo3 = new Combo(UUID.randomUUID(), vendedorId, "Combo A", "Desc A", 10.0, List.of(p1, p2));

        assertEquals(combo1, combo2);
        assertEquals(combo1.hashCode(), combo2.hashCode());
        assertNotEquals(combo1, combo3);
    }
}
