package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FiltroProductoTest {

    private Producto producto(String nombre, String categoria, Gama gama, double precio) {
        return Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc",
                precio, 5, gama, Exclusividad.NORMAL, categoria);
    }

    @Test
    void sinFiltrosCoincideConTodo() {
        assertTrue(FiltroProducto.sinFiltros().coincide(producto("Mouse", "Perifericos", Gama.BAJA, 10)));
    }

    @Test
    void rechazaRangoDePreciosInvertido() {
        assertThrows(ReglaDominioException.class,
                () -> new FiltroProducto(null, null, null, 100.0, 50.0, false));
    }

    @Test
    void rechazaPreciosNegativos() {
        assertThrows(ReglaDominioException.class,
                () -> new FiltroProducto(null, null, null, -1.0, null, false));
        assertThrows(ReglaDominioException.class,
                () -> new FiltroProducto(null, null, null, null, -1.0, false));
    }

    @Test
    void rangoDePreciosEsInclusivo() {
        FiltroProducto filtro = new FiltroProducto(null, null, null, 50.0, 100.0, false);

        assertTrue(filtro.coincide(producto("A", "X", Gama.BAJA, 50)));
        assertTrue(filtro.coincide(producto("B", "X", Gama.BAJA, 100)));
        assertFalse(filtro.coincide(producto("C", "X", Gama.BAJA, 49.99)));
        assertFalse(filtro.coincide(producto("D", "X", Gama.BAJA, 100.01)));
    }

    @Test
    void textoBuscaPorNombreSinImportarMayusculas() {
        FiltroProducto filtro = new FiltroProducto("mOuSe", null, null, null, null, false);

        assertTrue(filtro.coincide(producto("Mouse Gamer", "X", Gama.BAJA, 10)));
        assertFalse(filtro.coincide(producto("Teclado", "X", Gama.BAJA, 10)));
    }

    @Test
    void categoriaYGamaDebenCoincidirExactamente() {
        FiltroProducto filtro = new FiltroProducto(null, "Perifericos", Gama.ALTA, null, null, false);

        assertTrue(filtro.coincide(producto("A", "perifericos", Gama.ALTA, 10)));
        assertFalse(filtro.coincide(producto("B", "Perifericos", Gama.MEDIA, 10)));
        assertFalse(filtro.coincide(producto("C", "Sillas", Gama.ALTA, 10)));
    }
}
