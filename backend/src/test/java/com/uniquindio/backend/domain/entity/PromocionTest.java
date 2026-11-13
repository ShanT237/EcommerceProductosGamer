package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.valueobject.Precio;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromocionTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    @Test
    void programaUnaPromocionValida() {
        UUID id = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();

        Promocion p = Promocion.programar(id, productoId, 20, DESDE, HASTA);

        assertEquals(id, p.getId());
        assertEquals(productoId, p.getProductoId());
        assertEquals(20, p.getPorcentaje());
    }

    @Test
    void rechazaPorcentajeFueraDeRango() {
        assertThrows(ReglaDominioException.class,
                () -> Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 0, DESDE, HASTA));
        assertThrows(ReglaDominioException.class,
                () -> Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 100, DESDE, HASTA));
        assertThrows(ReglaDominioException.class,
                () -> Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), -5, DESDE, HASTA));
    }

    @Test
    void rechazaRangoDeFechasInvertido() {
        assertThrows(ReglaDominioException.class,
                () -> Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 10, HASTA, DESDE));
    }

    @Test
    void estaVigenteSoloEntreLasFechasInclusive() {
        Promocion p = Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 10, DESDE, HASTA);

        assertFalse(p.estaVigente(DESDE.minusDays(1)));
        assertTrue(p.estaVigente(DESDE));
        assertTrue(p.estaVigente(HASTA));
        assertFalse(p.estaVigente(HASTA.plusDays(1)));
    }

    @Test
    void detectaSolapamientoDeRangos() {
        Promocion p = Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 10, DESDE, HASTA);

        assertTrue(p.seSolapaCon(HASTA, HASTA.plusDays(5)));
        assertTrue(p.seSolapaCon(DESDE.minusDays(5), DESDE));
        assertFalse(p.seSolapaCon(HASTA.plusDays(1), HASTA.plusDays(5)));
    }

    @Test
    void validaSolapamientosYAplicaElDescuento() {
        UUID productoId = UUID.randomUUID();
        Promocion existente = Promocion.programar(UUID.randomUUID(), productoId, 10, DESDE, HASTA);
        Promocion solapada = Promocion.programar(UUID.randomUUID(), productoId, 20, HASTA, HASTA.plusDays(5));
        Promocion consecutiva = Promocion.programar(UUID.randomUUID(), productoId, 20,
                HASTA.plusDays(1), HASTA.plusDays(5));

        assertThrows(ConflictoException.class,
                () -> solapada.asegurarSinSolapamiento(List.of(existente)));
        assertDoesNotThrow(() -> consecutiva.asegurarSinSolapamiento(List.of(existente)));
        assertEquals(90.0, existente.aplicarA(new Precio(100.0)).valor(), 0.001);
    }
}
