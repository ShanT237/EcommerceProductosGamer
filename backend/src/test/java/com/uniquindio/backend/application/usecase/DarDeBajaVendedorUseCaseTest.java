package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DarDeBajaVendedorUseCaseTest {

    private VendedorRepositoryEnMemoria vendedorRepository;
    private DarDeBajaVendedorUseCase useCase;
    private UUID id;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new DarDeBajaVendedorUseCase(vendedorRepository);
        id = UUID.randomUUID();
        vendedorRepository.guardar(Vendedor.registrar(id, "GamerZone", "a@b.com", TipoVendedor.EMPRESA, "900123"));
    }

    @Test
    void daDeBajaYGuarda() {
        assertFalse(useCase.ejecutar(id).isActivo());
        assertFalse(vendedorRepository.obtenerPorId(id).get().isActivo());
    }

    @Test
    void segundaBajaLanzaReglaDominio() {
        useCase.ejecutar(id);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id));
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
