package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.security.BCryptHashContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CambiarContrasenaVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private BCryptHashContrasena hasher;
    private CambiarContrasenaVendedorUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        hasher = new BCryptHashContrasena();
        useCase = new CambiarContrasenaVendedorUseCase(vendedorRepository, hasher);
        id = UUID.randomUUID();
        new RegistrarVendedorUseCase(vendedorRepository, hasher)
                .ejecutar(id, "GamerZone", "ventas@gamerzone.com", TipoVendedor.EMPRESA, "900123", "Vendedor2026");
    }

    private Vendedor vendedor() {
        return vendedorRepository.obtenerPorId(id).get();
    }

    @Test
    void cambiaLaContrasenaYLaNuevaEsLaQueValida() {
        useCase.ejecutar(id, "Vendedor2026", "Nueva2027x");

        assertTrue(vendedor().verificarContrasena("Nueva2027x", hasher));
        assertFalse(vendedor().verificarContrasena("Vendedor2026", hasher));
    }

    @Test
    void contrasenaActualIncorrectaSeRechazaYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Equivocada1", "Nueva2027x"));

        assertTrue(vendedor().verificarContrasena("Vendedor2026", hasher));
    }

    @Test
    void contrasenaNuevaDebilOIgualALaActualSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Vendedor2026", "debil"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Vendedor2026", "Vendedor2026"));

        assertTrue(vendedor().verificarContrasena("Vendedor2026", hasher));
    }

    @Test
    void vendedorSinContrasenaNoPuedeCambiarla() {
        UUID sinClave = UUID.randomUUID();
        vendedorRepository.guardar(Vendedor.registrar(sinClave, "Sin clave", "sc@test.com",
                TipoVendedor.PERSONA_NATURAL, "555"));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(sinClave, "Vendedor2026", "Nueva2027x"));
    }

    @Test
    void vendedorDadoDeBajaOInexistenteRechaza() {
        vendedor().darDeBaja();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, "Vendedor2026", "Nueva2027x"));
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), "Vendedor2026", "Nueva2027x"));
    }
}
