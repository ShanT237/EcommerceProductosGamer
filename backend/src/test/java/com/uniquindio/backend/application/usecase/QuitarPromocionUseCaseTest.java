package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PromocionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuitarPromocionUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    private ProductoRepositoryEnMemoria productoRepository;
    private PromocionRepositoryEnMemoria promocionRepository;
    private QuitarPromocionUseCase useCase;
    private UUID vendedorId;
    private Producto producto;
    private Promocion promocion;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        promocionRepository = new PromocionRepositoryEnMemoria();
        useCase = new QuitarPromocionUseCase(productoRepository, promocionRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        promocion = Promocion.programar(UUID.randomUUID(), producto.getId(), 20, DESDE, HASTA);
        promocionRepository.guardar(promocion);
    }

    @Test
    void eliminaLaPromocion() {
        useCase.ejecutar(promocion.getId(), vendedorId);

        assertTrue(promocionRepository.obtenerPorId(promocion.getId()).isEmpty());
        assertTrue(promocionRepository.obtenerVigentePorProductoId(producto.getId(), DESDE).isEmpty());
    }

    @Test
    void noAfectaALasOtrasPromocionesDelProducto() {
        Promocion otra = Promocion.programar(UUID.randomUUID(), producto.getId(), 10,
                HASTA.plusDays(1), HASTA.plusDays(10));
        promocionRepository.guardar(otra);

        useCase.ejecutar(promocion.getId(), vendedorId);

        assertTrue(promocionRepository.obtenerPorId(otra.getId()).isPresent());
        assertEquals(1, promocionRepository.obtenerPorProductoId(producto.getId()).size());
    }

    @Test
    void noCambiaElPrecioBaseDelProducto() {
        useCase.ejecutar(promocion.getId(), vendedorId);

        assertEquals(100, producto.getPrecio());
    }

    @Test
    void puedeQuitarUnaPromocionVencida() {
        Promocion vencida = Promocion.programar(UUID.randomUUID(), producto.getId(), 15,
                DESDE.minusYears(1), HASTA.minusYears(1));
        promocionRepository.guardar(vencida);

        assertDoesNotThrow(() -> useCase.ejecutar(vencida.getId(), vendedorId));
        assertTrue(promocionRepository.obtenerPorId(vencida.getId()).isEmpty());
    }

    @Test
    void puedeQuitarLaPromocionDeUnProductoEliminado() {
        producto.eliminar(false);

        assertDoesNotThrow(() -> useCase.ejecutar(promocion.getId(), vendedorId));
        assertTrue(promocionRepository.obtenerPorId(promocion.getId()).isEmpty());
    }

    @Test
    void soloElDuenoQuitaLaPromocionYNoSeBorra() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(promocion.getId(), UUID.randomUUID()));

        assertTrue(promocionRepository.obtenerPorId(promocion.getId()).isPresent());
    }

    @Test
    void promocionInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));
    }
}
