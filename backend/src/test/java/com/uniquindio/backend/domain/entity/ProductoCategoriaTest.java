package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductoCategoriaTest {

    @Test
    void publicarConCategoriaLaGuarda() {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Desc",
                10, 1, Gama.BAJA, Exclusividad.NORMAL, "Perifericos");

        assertEquals("Perifericos", p.getCategoria());
    }

    @Test
    void publicarSinCategoriaUsaLaCategoriaPorDefecto() {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Desc",
                10, 1, Gama.BAJA, Exclusividad.NORMAL);

        assertEquals(Producto.CATEGORIA_POR_DEFECTO, p.getCategoria());
    }

    @Test
    void rechazaCategoriaVacia() {
        assertThrows(ReglaDominioException.class, () -> Producto.publicar(UUID.randomUUID(), UUID.randomUUID(),
                "Mouse", "Desc", 10, 1, Gama.BAJA, Exclusividad.NORMAL, " "));
    }

    @Test
    void estaVisibleSoloSiNoEstaEliminadoNiPausado() {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Desc",
                10, 1, Gama.BAJA, Exclusividad.NORMAL);
        assertTrue(p.estaVisible());

        p.pausar();
        assertFalse(p.estaVisible());
    }
}
