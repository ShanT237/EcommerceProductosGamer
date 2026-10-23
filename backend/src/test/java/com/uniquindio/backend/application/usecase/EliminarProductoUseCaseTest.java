package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EliminarProductoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private EliminarProductoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        useCase = new EliminarProductoUseCase(productoRepository, compraRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void elDuenoEliminaLogicamenteSuProducto() {
        useCase.ejecutar(producto.getId(), vendedorId);

        assertTrue(productoRepository.obtenerPorId(producto.getId()).get().isEliminado());
    }

    @Test
    void unVendedorAjenoNoPuedeEliminarElProducto() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID()));

        assertFalse(productoRepository.obtenerPorId(producto.getId()).get().isEliminado());
    }

    @Test
    void conComprasActivasSeRechazaLaEliminacion() {
        compraRepository.guardar(Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), producto.getId(),
                1, LocalDateTime.of(2026, 10, 1, 10, 0), new Precio(100)));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId));

        assertFalse(productoRepository.obtenerPorId(producto.getId()).get().isEliminado());
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));
    }
}