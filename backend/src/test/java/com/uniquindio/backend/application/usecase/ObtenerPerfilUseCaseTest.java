package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ObtenerPerfilUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private ObtenerPerfilUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        useCase = new ObtenerPerfilUseCase(usuarioRepository);
        id = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(id, "Gamer", "gamer@test.com"));
    }

    @Test
    void devuelveNombreYCorreoDelUsuario() {
        Usuario u = useCase.ejecutar(id);

        assertEquals("Gamer", u.getNombre());
        assertEquals("gamer@test.com", u.getCorreo());
    }

    @Test
    void usuarioInexistenteLanzaExcepcion() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
