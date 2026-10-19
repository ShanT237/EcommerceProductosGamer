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

class ModificarProductoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ModificarProductoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ModificarProductoUseCase(productoRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void cambiaElPrecioYGuarda() {
        Producto modificado = useCase.ejecutar(producto.getId(), vendedorId, 80);

        assertEquals(80, modificado.getPrecio());
        assertEquals(80, productoRepository.obtenerPorId(producto.getId()).get().getPrecio());
    }

    @Test
    void rechazaPrecioCeroONegativoYNoCambiaElPrecio() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, 0));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, -5));
        assertEquals(100, producto.getPrecio());
    }

    @Test
    void soloElDuenoModificaElProducto() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), 80));
        assertEquals(100, producto.getPrecio());
    }

    @Test
    void productoEliminadoNoSeModifica() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, 80));
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, 80));
    }
}
