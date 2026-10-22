package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.security.BCryptHashContrasena;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistrarVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private BCryptHashContrasena hasher;
    private RegistrarVendedorUseCase useCase;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        hasher = new BCryptHashContrasena();
        useCase = new RegistrarVendedorUseCase(vendedorRepository, hasher);
    }

    @Test
    void registraYGuarda() {
        UUID id = UUID.randomUUID();

        Vendedor v = useCase.ejecutar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123", "Vendedor2026");

        assertEquals(id, v.getId());
        assertTrue(vendedorRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void guardaSoloElHashDeLaContrasena() {
        UUID id = UUID.randomUUID();

        useCase.ejecutar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123", "Vendedor2026");

        Vendedor guardado = vendedorRepository.obtenerPorId(id).get();
        assertTrue(guardado.verificarContrasena("Vendedor2026", hasher));
        assertFalse(guardado.verificarContrasena("Otra2026xx", hasher));
    }

    @Test
    void documentoDuplicadoLanzaReglaDominioYNoGuarda() {
        useCase.ejecutar(UUID.randomUUID(), "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123", "Vendedor2026");
        UUID otroId = UUID.randomUUID();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(otroId, "Otro", "c@d.com", TipoVendedor.EMPRESA, "900123", "Vendedor2026"));

        assertEquals("Ya existe un vendedor con ese documento", ex.getMessage());
        assertTrue(vendedorRepository.obtenerPorId(otroId).isEmpty());
    }

    @Test
    void contrasenaDebilLanzaReglaDominioYNoGuarda() {
        UUID id = UUID.randomUUID();

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123", "debil"));

        assertTrue(vendedorRepository.obtenerPorId(id).isEmpty());
    }
}