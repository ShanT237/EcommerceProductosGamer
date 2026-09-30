package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    @DisplayName("Crea usuario con valores válidos, acumula y redime puntos correctamente")
    void acumulaYRedimePuntos() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Gamer1", "gamer@test.com");
        assertTrue(usuario.isActivo());
        assertEquals(0, usuario.getPuntos());

        usuario.acumularPuntos(50);
        assertEquals(50, usuario.getPuntos());

        usuario.redimirPuntos(30);
        assertEquals(20, usuario.getPuntos());

        assertThrows(ReglaDominioException.class, () -> usuario.redimirPuntos(50));
        assertThrows(ReglaDominioException.class, () -> usuario.redimirPuntos(0));
    }

    @Test
    @DisplayName("Permite desactivar y activar un usuario")
    void estadoActivoEInactivo() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Gamer2", "gamer2@test.com");
        assertTrue(usuario.isActivo());

        usuario.desactivar();
        assertFalse(usuario.isActivo());

        usuario.activar();
        assertTrue(usuario.isActivo());
    }
}
