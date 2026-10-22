package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.AutenticacionException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.valueobject.Rol;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
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
    private VendedorRepositoryEnMemoria vendedorRepository;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();   // <- sin "VendedorRepositoryEnMemoria" delante
        hasher = new BCryptHashContrasena();
        emisor = new JwtEmisorToken("secreto-de-prueba-con-mas-de-32-caracteres", 2, Clock.systemUTC());
        useCase = new IniciarSesionUseCase(usuarioRepository, vendedorRepository, hasher, emisor);

        id = UUID.randomUUID();
        new RegistrarUsuarioUseCase(usuarioRepository, hasher).ejecutar(id, "Gamer", "gamer@test.com", "Gamer2026");

        vendedorId = UUID.randomUUID();
        Vendedor vendedor = Vendedor.registrar(vendedorId, "GamerZone", "ventas@gamerzone.com",
                TipoVendedor.EMPRESA, "900123");
        vendedor.definirContrasena(hasher.hashear("Vendedor2026"));
        vendedorRepository.guardar(vendedor);
    }

    @Test
    void credencialesValidasDevuelvenUnTokenDelUsuario() {
        String token = useCase.ejecutar("gamer@test.com", "Gamer2026");

        SesionToken sesion = emisor.validar(token).orElseThrow();
        assertEquals(id, sesion.usuarioId());
        assertEquals(Rol.COMPRADOR, sesion.rol());
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

    @Test
    void vendedorConCredencialesValidasRecibeTokenConRolVendedor() {
        String token = useCase.ejecutar("ventas@gamerzone.com", "Vendedor2026");

        SesionToken sesion = emisor.validar(token).orElseThrow();
        assertEquals(vendedorId, sesion.usuarioId());
        assertEquals(Rol.VENDEDOR, sesion.rol());
    }

    @Test
    void contrasenaIncorrectaDeVendedorLanzaAutenticacion() {
        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("ventas@gamerzone.com", "Otra2026x"));
    }

    @Test
    void vendedorSinContrasenaNoPuedeIniciarSesion() {
        vendedorRepository.guardar(Vendedor.registrar(UUID.randomUUID(), "Sin clave", "sin@vendedor.com",
                TipoVendedor.PERSONA_NATURAL, "555"));

        assertThrows(AutenticacionException.class, () -> useCase.ejecutar("sin@vendedor.com", "Vendedor2026"));
    }

    @Test
    void vendedorDadoDeBajaNoPuedeIniciarSesion() {
        vendedorRepository.obtenerPorId(vendedorId).get().darDeBaja();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar("ventas@gamerzone.com", "Vendedor2026"));
    }
}
