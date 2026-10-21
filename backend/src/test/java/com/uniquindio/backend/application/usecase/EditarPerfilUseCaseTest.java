package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EditarPerfilUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private EditarPerfilUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        useCase = new EditarPerfilUseCase(usuarioRepository);
        id = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(id, "Gamer", "gamer@test.com"));
        usuarioRepository.guardar(Usuario.registrar(UUID.randomUUID(), "Otro", "otro@test.com"));
    }

    @Test
    void cambiaNombreYCorreoYGuarda() {
        Usuario u = useCase.ejecutar(id, "Gamer Pro", "pro@test.com");

        assertEquals("Gamer Pro", u.getNombre());
        assertEquals("pro@test.com", usuarioRepository.obtenerPorId(id).get().getCorreo());
    }

    @Test
    void permiteConservarElPropioCorreo() {
        assertDoesNotThrow(() -> useCase.ejecutar(id, "Nuevo Nombre", "GAMER@test.com"));
        assertEquals("Nuevo Nombre", usuarioRepository.obtenerPorId(id).get().getNombre());
    }

    @Test
    void correoDeOtroUsuarioSeRechazaYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Nuevo", "otro@test.com"));

        assertEquals("Gamer", usuarioRepository.obtenerPorId(id).get().getNombre());
        assertEquals("gamer@test.com", usuarioRepository.obtenerPorId(id).get().getCorreo());
    }

    @Test
    void datosInvalidosNoCambianNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, " ", "nuevo@test.com"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Nuevo", "sin-arroba"));

        assertEquals("Gamer", usuarioRepository.obtenerPorId(id).get().getNombre());
        assertEquals("gamer@test.com", usuarioRepository.obtenerPorId(id).get().getCorreo());
    }

    @Test
    void usuarioInactivoOInexistenteRechaza() {
        usuarioRepository.obtenerPorId(id).get().desactivar();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Nuevo", "nuevo@test.com"));
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), "Nuevo", "nuevo@test.com"));
    }
}
