package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoVendedorTest {

    @Test
    void personaNaturalNoDebeSerEmpresa() {
        assertFalse(TipoVendedor.PERSONA_NATURAL.esEmpresa());
    }

    @Test
    void empresaDebeSerEmpresa() {
        assertTrue(TipoVendedor.EMPRESA.esEmpresa());
    }

    @Test
    void cadaTipoDebeTenerSuTipoDeDocumento() {
        assertEquals("Cédula", TipoVendedor.PERSONA_NATURAL.tipoDocumento());
        assertEquals("NIT", TipoVendedor.EMPRESA.tipoDocumento());
    }

    @Test
    void debeTenerExactamenteDosConstantes() {
        assertEquals(2, TipoVendedor.values().length);
    }
}