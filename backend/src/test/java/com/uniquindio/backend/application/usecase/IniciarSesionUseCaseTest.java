package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.AutenticacionException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.security.JwtEmisorToken;
import com.uniquindio.backend.infrastructure.security.BCryptHashContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class IniciarSesionUseCaseTest {

    private UsuarioRepositoryEnMemoria usuarioRepository;
    private BCryptHashContrasena hasher;
    private JwtEmisorToken emisor;
    private IniciarSesionUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        hasher = new BCryptHashContrasena();
        emisor = new JwtEmisorToken("secreto-de-prueba-con-mas-de-32-caracteres", 2, Clock.systemUTC());
        useCase = new IniciarSesionUseCase(usuarioRepository, hasher, emisor);
        id = UUID.randomUUID();
        new RegistrarUsuarioUseCase(usuarioRepository, hasher).ejecutar(id, "Gamer", "gamer@test.com", "Gamer2026");
    }

    @Test
    void credencialesValidasDevuelvenUnTokenDelUsuario() {
        String token = useCase.ejecutar("gamer@test.com", "Gamer2026");

        assertEquals(id, emisor.validar(token).orElseThrow());
    }

    @Test
    void elCorreoNoDistingueMayusculas() {
        assertDoesNotThrow(() -> useCase.ejecutar("GAMER@test.com", "Gamer2026"));
    }

    @Test
    void contrasenaIncorrectaOUsuarioInexistenteLanzanAutenticacion() {
        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("gamer@test.com", "Otra2026x"));
        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("nadie@test.com", "Gamer2026"));
        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("gamer@test.com", ""));
        assertThrows(AutenticacionException.class, () -> useCase.ejecutar(null, "Gamer2026"));
    }

    @Test
    void usuarioSinContrasenaNoPuedeIniciarSesion() {
        usuarioRepository.guardar(Usuario.registrar(UUID.randomUUID(), "Sin clave", "sin@test.com"));

        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("sin@test.com", "Gamer2026"));
    }

    @Test
    void usuarioInactivoNoPuedeIniciarSesion() {
        usuarioRepository.obtenerPorId(id).get().desactivar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar("gamer@test.com", "Gamer2026"));
    }

    @Test
    void registrarConContrasenaDebilLanzaReglaDominioYNoGuarda() {
        UUID otro = UUID.randomUUID();

        assertThrows(ReglaDominioException.class, () -> new RegistrarUsuarioUseCase(usuarioRepository, hasher)
                .ejecutar(otro, "Otro", "otro@test.com", "debil"));
        assertTrue(usuarioRepository.obtenerPorId(otro).isEmpty());
    }
}
