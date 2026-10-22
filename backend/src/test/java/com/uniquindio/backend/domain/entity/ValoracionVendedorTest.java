package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ValoracionVendedorTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private ValoracionVendedor crear(boolean completada, Calificacion cal, String comentario) {
        return ValoracionVendedor.crear(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), completada, cal, comentario, FECHA);
    }

    @Test
    void creaUnaValoracionValidaConComentarioOpcional() {
        ValoracionVendedor v = crear(true, new Calificacion(4), null);

        assertEquals(4, v.getCalificacion().estrellas());
        assertEquals("", v.getComentario());
        assertEquals(FECHA, v.getFecha());
    }

    @Test
    void exigeCompraCompletadaYCalificacion() {
        assertThrows(ReglaDominioException.class, () -> crear(false, new Calificacion(4), "Bien"));
        assertThrows(ReglaDominioException.class, () -> crear(true, null, "Bien"));
    }

    @Test
    void elComentarioNoPuedeSuperar500Caracteres() {
        assertDoesNotThrow(() -> crear(true, new Calificacion(5), "a".repeat(500)));
        assertThrows(ReglaDominioException.class, () -> crear(true, new Calificacion(5), "a".repeat(501)));
    }

    @Test
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        ValoracionVendedor a = ValoracionVendedor.crear(id, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), true, new Calificacion(5), "A", FECHA);
        ValoracionVendedor b = ValoracionVendedor.crear(id, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), true, new Calificacion(1), "B", FECHA);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }
}
