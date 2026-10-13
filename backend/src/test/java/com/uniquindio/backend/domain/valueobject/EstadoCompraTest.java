package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EstadoCompraTest {

    private static final Map<EstadoCompra, Set<EstadoCompra>> PERMITIDAS = Map.of(
            EstadoCompra.PENDIENTE, EnumSet.of(EstadoCompra.COMPLETADA, EstadoCompra.CANCELADA, EstadoCompra.EXPIRADA),
            EstadoCompra.COMPLETADA, EnumSet.of(EstadoCompra.REEMBOLSADA),
            EstadoCompra.REEMBOLSADA, EnumSet.noneOf(EstadoCompra.class),
            EstadoCompra.CANCELADA, EnumSet.noneOf(EstadoCompra.class),
            EstadoCompra.EXPIRADA, EnumSet.noneOf(EstadoCompra.class)
    );

    @Test
    void matrizCompletaDeTransiciones() {
        for (EstadoCompra origen : EstadoCompra.values()) {
            for (EstadoCompra destino : EstadoCompra.values()) {
                assertEquals(PERMITIDAS.get(origen).contains(destino),
                        origen.puedeTransicionarA(destino),
                        origen + " -> " + destino);
            }
        }
    }

    @Test
    void soloReembolsadaCanceladaYExpiradaSonFinales() {
        assertFalse(EstadoCompra.PENDIENTE.esFinal());
        assertFalse(EstadoCompra.COMPLETADA.esFinal());
        assertTrue(EstadoCompra.REEMBOLSADA.esFinal());
        assertTrue(EstadoCompra.CANCELADA.esFinal());
        assertTrue(EstadoCompra.EXPIRADA.esFinal());
    }
}