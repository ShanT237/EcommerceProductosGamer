package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistrarUsuarioUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private RegistrarUsuarioUseCase useCase;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        useCase = new RegistrarUsuarioUseCase(usuarioRepository, new com.uniquindio.backend.infrastructure.security.Pbkdf2HashContrasena());
    }

    @Test
    void registraYGuarda() {
        UUID id = UUID.randomUUID();

        Usuario u = useCase.ejecutar(id, "Gamer1", "gamer@test.com");

        assertEquals(0, u.getPuntos());
        assertTrue(usuarioRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void correoDuplicadoLanzaReglaDominioYNoGuarda() {
        useCase.ejecutar(UUID.randomUUID(), "Gamer1", "gamer@test.com");
        UUID otroId = UUID.randomUUID();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(otroId, "Gamer2", "gamer@test.com"));

        assertEquals("Ya existe un usuario con ese correo", ex.getMessage());
        assertTrue(usuarioRepository.obtenerPorId(otroId).isEmpty());
    }
}
