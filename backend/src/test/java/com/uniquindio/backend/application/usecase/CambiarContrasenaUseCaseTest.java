package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.security.BCryptHashContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CambiarContrasenaUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private BCryptHashContrasena hasher;
    private CambiarContrasenaUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        hasher = new BCryptHashContrasena();
        useCase = new CambiarContrasenaUseCase(usuarioRepository, hasher);
        id = UUID.randomUUID();
        new RegistrarUsuarioUseCase(usuarioRepository, hasher).ejecutar(id, "Gamer", "g@test.com", "Gamer2026");
    }

    private Usuario usuario() {
        return usuarioRepository.obtenerPorId(id).get();
    }

    @Test
    void cambiaLaContrasenaYLaNuevaEsLaQueValida() {
        useCase.ejecutar(id, "Gamer2026", "Nueva2027x");

        assertTrue(usuario().verificarContrasena("Nueva2027x", hasher));
        assertFalse(usuario().verificarContrasena("Gamer2026", hasher));
    }

    @Test
    void contrasenaActualIncorrectaSeRechazaYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Equivocada1", "Nueva2027x"));

        assertTrue(usuario().verificarContrasena("Gamer2026", hasher));
    }

    @Test
    void contrasenaNuevaDebilOIgualALaActualSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Gamer2026", "debil"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Gamer2026", "Gamer2026"));

        assertTrue(usuario().verificarContrasena("Gamer2026", hasher));
    }

    @Test
    void usuarioSinContrasenaNoPuedeCambiarla() {
        UUID sinClave = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(sinClave, "Sin clave", "sc@test.com"));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(sinClave, "Gamer2026", "Nueva2027x"));
    }

    @Test
    void usuarioInactivoOInexistenteRechaza() {
        usuario().desactivar();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Gamer2026", "Nueva2027x"));
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), "Gamer2026", "Nueva2027x"));
    }
}
