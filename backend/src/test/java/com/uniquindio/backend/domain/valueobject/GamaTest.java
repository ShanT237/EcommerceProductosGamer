package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GamaTest {

    @Test
    void dosGamaSonIguales() {
        Gama g1 = Gama.ALTA;
        Gama g2 = Gama.ALTA;
        assertEquals(g1,g2);
        assertEquals(g1.mesesGarantia(), g2.mesesGarantia());
        assertFalse(g1.esSuperiorA(g2));
    }

    @Test
    void gamaSuperiorALaOtra() {
        Gama g1 = Gama.ALTA;
        Gama g2 = Gama.MEDIA;
        Gama g3 = Gama.BAJA;
        assertTrue(g1.esSuperiorA(g2));
        assertTrue(g1.esSuperiorA(g3));
        assertTrue(g2.esSuperiorA(g3));
    }
    @Test
    void gamaInferiorALaOtra() {
        Gama g1 = Gama.ALTA;
        Gama g2 = Gama.MEDIA;
        Gama g3 = Gama.BAJA;
        assertFalse(g2.esSuperiorA(g1));
        assertFalse(g3.esSuperiorA(g2));
        assertFalse(g3.esSuperiorA(g1));
    }
    
}
