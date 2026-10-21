package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegistrarVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private RegistrarVendedorUseCase useCase;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new RegistrarVendedorUseCase(vendedorRepository);
    }

    @Test
    void registraYGuarda() {
        UUID id = UUID.randomUUID();

        Vendedor v = useCase.ejecutar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123");

        assertEquals(id, v.getId());
        assertTrue(vendedorRepository.obtenerPorId(id).isPresent());
    }

    @Test
    void documentoDuplicadoLanzaReglaDominioYNoGuarda() {
        useCase.ejecutar(UUID.randomUUID(), "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123");
        UUID otroId = UUID.randomUUID();

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(otroId, "Otro", "c@d.com", TipoVendedor.EMPRESA, "900123"));

        assertEquals("Ya existe un vendedor con ese documento", ex.getMessage());
        assertTrue(vendedorRepository.obtenerPorId(otroId).isEmpty());
    }
}
