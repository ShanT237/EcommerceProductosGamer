package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FiltroProductoTest {

    @Test
    void todosLosFiltrosSonOpcionales() {
        FiltroProducto filtro = FiltroProducto.sinFiltros();

        assertNull(filtro.texto());
        assertNull(filtro.categoria());
        assertNull(filtro.gama());
        assertNull(filtro.precioMin());
        assertNull(filtro.precioMax());
        assertFalse(filtro.soloPreventa());
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
    void conservaCriteriosParaLaConsultaDelCatalogo() {
        FiltroProducto filtro = new FiltroProducto(
                "mouse", "Perifericos", Gama.ALTA, 50.0, 100.0, true);

        assertEquals("mouse", filtro.texto());
        assertEquals("Perifericos", filtro.categoria());
        assertEquals(Gama.ALTA, filtro.gama());
        assertEquals(50.0, filtro.precioMin());
        assertEquals(100.0, filtro.precioMax());
        assertTrue(filtro.soloPreventa());
    }
}
