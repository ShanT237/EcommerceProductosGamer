package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CuponTest {

    private static final LocalDate VENCE = LocalDate.of(2026, 12, 31);

    private Cupon cupon(int usosMaximos) {
        return Cupon.crear(UUID.randomUUID(), "gamer10", 10, VENCE, usosMaximos);
    }

    @Test
    void creaCuponNormalizandoElCodigoAMayusculas() {
        Cupon c = cupon(5);

        assertEquals("GAMER10", c.getCodigo());
        assertEquals(10, c.getPorcentaje());
        assertEquals(0, c.getUsos());
    }

    @Test
    void rechazaDatosInvalidos() {
        UUID id = UUID.randomUUID();
        assertThrows(ReglaDominioException.class, () -> Cupon.crear(id, " ", 10, VENCE, 1));
        assertThrows(ReglaDominioException.class, () -> Cupon.crear(id, "X", 0, VENCE, 1));
        assertThrows(ReglaDominioException.class, () -> Cupon.crear(id, "X", 100, VENCE, 1));
        assertThrows(ReglaDominioException.class, () -> Cupon.crear(id, "X", 10, VENCE, 0));
    }

    @Test
    void esValidoHastaElDiaDeVencimientoInclusive() {
        Cupon c = cupon(1);

        assertDoesNotThrow(() -> c.validarUso(VENCE));
        assertThrows(ReglaDominioException.class, () -> c.validarUso(VENCE.plusDays(1)));
    }

    @Test
    void sinUsosDisponiblesNoSePuedeUsar() {
        Cupon c = cupon(1);
        c.registrarUso();

        assertThrows(ReglaDominioException.class, () -> c.validarUso(VENCE.minusDays(1)));
        assertEquals(1, c.getUsos());
    }

    @Test
    void elCuponSinDuenoEsGlobalYAplicaATodosLosVendedores() {
        Cupon global = cupon(5);

        assertNull(global.getVendedorId());
        assertTrue(global.aplicaA(UUID.randomUUID()));
    }

    @Test
    void aplicaASoloALosProductosDeSuDuenoOATodosSiEsGlobal() {
        UUID dueno = UUID.randomUUID();
        Cupon propio = Cupon.crear(UUID.randomUUID(), dueno, "tienda10", 10, VENCE, 5);

        assertEquals(dueno, propio.getVendedorId());
        assertEquals("TIENDA10", propio.getCodigo());
        assertTrue(propio.aplicaA(dueno));
        assertFalse(propio.aplicaA(UUID.randomUUID()));
        assertTrue(cupon(5).aplicaA(dueno));
    }

    @Test
    void usosDisponiblesDescuentaCadaUsoRegistrado() {
        Cupon c = cupon(3);
        assertEquals(3, c.usosDisponibles());

        c.registrarUso();

        assertEquals(2, c.usosDisponibles());
    }

    @Test
    void registrarUsoSinUsosDisponiblesLanzaReglaDominio() {
        Cupon c = cupon(1);
        c.registrarUso();

        assertThrows(ReglaDominioException.class, c::registrarUso);
        assertEquals(1, c.getUsos());
    }
}
