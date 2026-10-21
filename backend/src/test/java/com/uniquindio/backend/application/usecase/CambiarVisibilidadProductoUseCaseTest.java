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

class CambiarVisibilidadProductoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private CambiarVisibilidadProductoUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new CambiarVisibilidadProductoUseCase(productoRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void nuevoProductoEstaActivo() {
        assertFalse(producto.isPausado());
        assertDoesNotThrow(producto::asegurarComprable);
    }

    @Test
    void pausaYReactivaElProductoYGuarda() {
        assertTrue(useCase.ejecutar(producto.getId(), vendedorId, false).isPausado());
        assertTrue(productoRepository.obtenerPorId(producto.getId()).get().isPausado());

        assertFalse(useCase.ejecutar(producto.getId(), vendedorId, true).isPausado());
    }

    @Test
    void pausarDosVecesLanzaReglaDominio() {
        useCase.ejecutar(producto.getId(), vendedorId, false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, false));
    }

    @Test
    void activarUnProductoYaActivoLanzaReglaDominio() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, true));
    }

    @Test
    void productoPausadoNoEsComprable() {
        producto.pausar();

        assertThrows(ReglaDominioException.class, producto::asegurarComprable);
    }

    @Test
    void productoEliminadoNoSePausa() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), vendedorId, false));
    }

    @Test
    void soloElDuenoCambiaLaVisibilidad() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), false));
        assertFalse(producto.isPausado());
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, false));
    }
}
