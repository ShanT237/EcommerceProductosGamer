package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    @Test
    @DisplayName("Crea un usuario válido con puntos iniciales en 0, y falla con datos inválidos")
    void creaUsuarioValidoYValidaDatos() {
        UUID id = UUID.randomUUID();
        Usuario usuario = new Usuario(id, "Juan Pérez", "juan@example.com");

        assertEquals(id, usuario.getId());
        assertEquals("Juan Pérez", usuario.getNombre());
        assertEquals("juan@example.com", usuario.getCorreo());
        assertEquals(0, usuario.getPuntos());

        assertThrows(ReglaDominioException.class,
                () -> new Usuario(id, "", "juan@example.com"));
        assertThrows(ReglaDominioException.class,
                () -> new Usuario(id, "Juan", "correo-invalido.com"));
    }

    @Test
    @DisplayName("acumularPuntos suma correctamente y rechaza cantidades no positivas")
    void acumularPuntos() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Juan", "juan@example.com");

        usuario.acumularPuntos(10);
        usuario.acumularPuntos(5);
        assertEquals(15, usuario.getPuntos());

        assertThrows(ReglaDominioException.class, () -> usuario.acumularPuntos(0));
        assertThrows(ReglaDominioException.class, () -> usuario.acumularPuntos(-5));
    }

    @Test
    @DisplayName("La identidad del usuario depende solo del id")
    void identidadPorId() {
        UUID id = UUID.randomUUID();
        Usuario usuario1 = new Usuario(id, "Nombre A", "a@example.com");
        Usuario usuario2 = new Usuario(id, "Nombre B", "b@example.com");
        Usuario usuario3 = new Usuario(UUID.randomUUID(), "Nombre A", "a@example.com");

        assertEquals(usuario1, usuario2);
        assertEquals(usuario1.hashCode(), usuario2.hashCode());
        assertNotEquals(usuario1, usuario3);
    }
}

