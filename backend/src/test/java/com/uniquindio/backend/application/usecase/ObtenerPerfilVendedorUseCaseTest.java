package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ObtenerPerfilVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private ObtenerPerfilVendedorUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new ObtenerPerfilVendedorUseCase(vendedorRepository);
        id = UUID.randomUUID();
        vendedorRepository.guardar(Vendedor.registrar(id, "GamerZone", "ventas@gamerzone.com",
                TipoVendedor.EMPRESA, "900123"));
    }

    @Test
    void devuelveElPerfilDelVendedor() {
        Vendedor perfil = useCase.ejecutar(id);

        assertEquals("GamerZone", perfil.getNombre());
        assertEquals("ventas@gamerzone.com", perfil.getCorreo());
        assertEquals("900123", perfil.getDocumento());
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
