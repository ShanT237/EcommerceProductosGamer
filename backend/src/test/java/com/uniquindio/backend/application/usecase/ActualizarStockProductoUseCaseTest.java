package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ActualizarStockProductoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ActualizarStockProductoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ActualizarStockProductoUseCase(productoRepository, TestNotificador.nuevo());
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 0, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void reponeUnProductoAgotadoYGuarda() {
        Producto actualizado = useCase.ejecutar(producto.getId(), vendedorId, 20);

        assertEquals(20, actualizado.getStock());
        assertEquals(20, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }

    @Test
    void permiteDejarElStockEnCero() {
        useCase.ejecutar(producto.getId(), vendedorId, 5);

        assertEquals(0, useCase.ejecutar(producto.getId(), vendedorId, 0).getStock());
    }

    @Test
    void rechazaStockNegativoYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, -1));
        assertEquals(0, producto.getStock());
    }

    @Test
    void soloElDuenoActualizaElStock() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), 10));
        assertEquals(0, producto.getStock());
    }

    @Test
    void productoEliminadoNoSeModifica() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, 10));
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, 10));
    }
}
