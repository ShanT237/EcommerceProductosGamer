package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RolTest {

    @Test
    void compradorEsCompradorYNoVendedor() {
        assertTrue(Rol.COMPRADOR.esComprador());
        assertFalse(Rol.COMPRADOR.esVendedor());
    }

    @Test
    void vendedorEsVendedorYNoComprador() {
        assertTrue(Rol.VENDEDOR.esVendedor());
        assertFalse(Rol.VENDEDOR.esComprador());
    }

    @Test
    void debeTenerExactamenteDosConstantes() {
        assertEquals(2, Rol.values().length);
    }
}