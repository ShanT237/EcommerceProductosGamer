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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuitarDescuentoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private QuitarDescuentoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new QuitarDescuentoUseCase(productoRepository);

        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        producto.aplicarDescuento(20);
        productoRepository.guardar(producto);
    }

    @Test
    void elDuenoQuitaElDescuentoYSeGuarda() {
        Producto resultado = useCase.ejecutar(producto.getId(), vendedorId);

        assertEquals(100.0, resultado.getPrecio(), 0.001);
        Producto guardado = productoRepository.obtenerPorId(producto.getId()).orElseThrow();
        assertEquals(100.0, guardado.getPrecio(), 0.001);
        assertNull(guardado.getPrecioOriginal());
    }

    @Test
    void unVendedorAjenoRechazaYElDescuentoSigueActivo() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID()));

        Producto guardado = productoRepository.obtenerPorId(producto.getId()).orElseThrow();
        assertEquals(80.0, guardado.getPrecio(), 0.001);
        assertEquals(100.0, guardado.getPrecioOriginal());
    }

    @Test
    void sinDescuentoActivoRechaza() {
        useCase.ejecutar(producto.getId(), vendedorId);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId));
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));
    }
}
