package com.uniquindio.backend.domain.valueobject;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EstadoRegaloTest {

    private static final Map<EstadoRegalo, Set<EstadoRegalo>> PERMITIDAS = Map.of(
            EstadoRegalo.DISPONIBLE, EnumSet.of(EstadoRegalo.ELEGIDO, EstadoRegalo.CANCELADO),
            EstadoRegalo.ELEGIDO, EnumSet.of(EstadoRegalo.ENVIADO, EstadoRegalo.CANCELADO),
            EstadoRegalo.ENVIADO, EnumSet.of(EstadoRegalo.ENTREGADO, EstadoRegalo.CANCELADO),
            EstadoRegalo.ENTREGADO, EnumSet.noneOf(EstadoRegalo.class),
            EstadoRegalo.CANCELADO, EnumSet.noneOf(EstadoRegalo.class)
    );

    @Test
    void matrizCompletaDeTransiciones() {
        for (EstadoRegalo origen : EstadoRegalo.values()) {
            for (EstadoRegalo destino : EstadoRegalo.values()) {
                assertEquals(PERMITIDAS.get(origen).contains(destino),
                        origen.puedeTransicionarA(destino),
                        origen + " -> " + destino);
            }
        }
    }

    @Test
    void soloEntregadoYCanceladoSonFinales() {
        assertFalse(EstadoRegalo.DISPONIBLE.esFinal());
        assertFalse(EstadoRegalo.ELEGIDO.esFinal());
        assertFalse(EstadoRegalo.ENVIADO.esFinal());
        assertTrue(EstadoRegalo.ENTREGADO.esFinal());
        assertTrue(EstadoRegalo.CANCELADO.esFinal());
    }
}