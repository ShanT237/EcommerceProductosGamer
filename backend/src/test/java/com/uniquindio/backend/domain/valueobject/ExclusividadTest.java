package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExclusividadTest {

    @Test
    void normalNoDebeSerExclusivo() {
        assertFalse(Exclusividad.NORMAL.esExclusivo());
    }

    @Test
    void exclusivoDebeSerExclusivo() {
        assertTrue(Exclusividad.EXCLUSIVO.esExclusivo());
    }

    @Test
    void debeTenerExactamenteDosConstantes() {
        assertEquals(2, Exclusividad.values().length);
    }
}