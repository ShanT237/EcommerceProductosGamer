package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AlertasStockBajoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private AlertasStockBajoUseCase useCase;
    private Vendedor vendedor;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        VendedorRepositoryEnMemoria vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new AlertasStockBajoUseCase(vendedorRepository, productoRepository);
        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
    }

    private Producto producto(UUID dueno, String nombre, int stock) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, nombre, "Desc", 100, stock,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void listaLosProductosConStockHastaElUmbralInclusiveAgotadosPrimero() {
        Producto bajo = producto(vendedor.getId(), "Bajo", 3);
        Producto agotado = producto(vendedor.getId(), "Agotado", 0);
        producto(vendedor.getId(), "Normal", 4);

        assertEquals(List.of(agotado, bajo), useCase.ejecutar(vendedor.getId(), 3));
    }

    @Test
    void ignoraProductosEliminadosYDeOtrosVendedores() {
        producto(vendedor.getId(), "Eliminado", 0).eliminar(false);
        producto(UUID.randomUUID(), "Ajeno", 0);

        assertTrue(useCase.ejecutar(vendedor.getId(), 5).isEmpty());
    }

    @Test
    void umbralNoPositivoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(vendedor.getId(), 0));
    }

    @Test
    void vendedorInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), 5));
    }
}
