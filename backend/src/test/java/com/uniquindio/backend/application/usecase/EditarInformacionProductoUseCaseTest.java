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

class EditarInformacionProductoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private EditarInformacionProductoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new EditarInformacionProductoUseCase(productoRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void editaNombreYDescripcionYGuarda() {
        Producto editado = useCase.ejecutar(producto.getId(), vendedorId, "Mouse Pro", "Mouse inalámbrico");

        assertEquals("Mouse Pro", editado.getNombre());
        assertEquals("Mouse inalámbrico", productoRepository.obtenerPorId(producto.getId()).get().getDescripcion());
    }

    @Test
    void rechazaNombreODescripcionVaciosSinCambiarNada() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId, " ", "Desc"));
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId, "Nombre", null));
        assertEquals("Mouse", producto.getNombre());
        assertEquals("Mouse RGB", producto.getDescripcion());
    }

    @Test
    void soloElDuenoEdita() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), "Otro", "Otro"));
        assertEquals("Mouse", producto.getNombre());
    }

    @Test
    void productoEliminadoNoSeEdita() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId, "Otro", "Otro"));
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, "Otro", "Otro"));
    }
}
