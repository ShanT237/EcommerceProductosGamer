package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegaloTest {

    @Test
    @DisplayName("Crea un regalo disponible y falla si no tiene producto ni combo")
    void creaRegaloValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        UUID idProducto = UUID.randomUUID();

        Regalo regalo = Regalo.crear(id, idProducto, null);

        assertEquals(id, regalo.getId());
        assertEquals(EstadoRegalo.DISPONIBLE, regalo.getEstado());
        assertNull(regalo.getIdUsuario());

        assertThrows(ReglaDominioException.class,
                () -> Regalo.crear(UUID.randomUUID(), null, null));
    }

    @Test
    @DisplayName("Elige, envía y confirma la entrega en el orden correcto")
    void flujoCompletoDeEntrega() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        UUID idUsuario = UUID.randomUUID();

        regalo.elegir(idUsuario, false);
        assertEquals(EstadoRegalo.ELEGIDO, regalo.getEstado());

        regalo.enviar();
        assertEquals(EstadoRegalo.ENVIADO, regalo.getEstado());

        regalo.confirmarEntrega();
        assertEquals(EstadoRegalo.ENTREGADO, regalo.getEstado());
        assertNotNull(regalo.getFechaEntrega());
    }

    @Test   
    @DisplayName("Rechaza enviar un regalo que no ha sido elegido, y el estado no cambia")
    void rechazaEnviarSinElegir() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThrows(ReglaDominioException.class, regalo::enviar);
        assertEquals(EstadoRegalo.DISPONIBLE, regalo.getEstado());
    }

    @Test
    @DisplayName("Rechaza elegir un regalo a un usuario que ya posee el producto, y el estado no cambia")
    void rechazaElegirSiUsuarioYaPoseeElProducto() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        UUID idUsuario = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> regalo.elegir(idUsuario, true));
        assertEquals(EstadoRegalo.DISPONIBLE, regalo.getEstado());
        assertNull(regalo.getIdUsuario());
    }

    @Test
    @DisplayName("Un regalo entregado no puede cambiar de estado nunca más")
    void rechazaCambiarEstadoDespuesDeEntregado() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        regalo.elegir(UUID.randomUUID(), false);
        regalo.enviar();
        regalo.confirmarEntrega();

        assertThrows(ReglaDominioException.class, regalo::cancelarRegalo);
        assertEquals(EstadoRegalo.ENTREGADO, regalo.getEstado());
    }

    @Test
    @DisplayName("La identidad del regalo depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Regalo regalo1 = Regalo.crear(id, UUID.randomUUID(), null);
        Regalo regalo2 = Regalo.crear(id, UUID.randomUUID(), null);
        Regalo regalo3 = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);

        assertEquals(regalo1, regalo2);
        assertEquals(regalo1.hashCode(), regalo2.hashCode());
        assertNotEquals(regalo1, regalo3);
    }
}