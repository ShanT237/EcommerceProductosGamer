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

class ConfigurarDescuentoPromocionalUseCaseTest {

    private static final LocalDate DESDE = LocalDate.of(2026, 12, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 12, 31);

    private ProductoRepositoryEnMemoria productoRepository;
    private PromocionRepositoryEnMemoria promocionRepository;
    private ConfigurarDescuentoPromocionalUseCase useCase;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        promocionRepository = new PromocionRepositoryEnMemoria();
        useCase = new ConfigurarDescuentoPromocionalUseCase(productoRepository, promocionRepository);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    void programaLaPromocionYNoCambiaElPrecioBase() {
        Promocion p = useCase.ejecutar(producto.getId(), vendedorId, 20, DESDE, HASTA);

        assertEquals(20, p.getPorcentaje());
        assertTrue(promocionRepository.obtenerVigentePorProductoId(producto.getId(), DESDE).isPresent());
        assertTrue(promocionRepository.obtenerVigentePorProductoId(producto.getId(), HASTA.plusDays(1)).isEmpty());
        assertEquals(100, producto.getPrecio());
    }

    @Test
    void rechazaPromocionSolapadaConOtraDelMismoProducto() {
        useCase.ejecutar(producto.getId(), vendedorId, 20, DESDE, HASTA);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId, 30, HASTA, HASTA.plusDays(7)));
    }

    @Test
    void permitePromocionesConsecutivasSinSolapar() {
        useCase.ejecutar(producto.getId(), vendedorId, 20, DESDE, HASTA);

        assertDoesNotThrow(() -> useCase.ejecutar(producto.getId(), vendedorId, 30,
                HASTA.plusDays(1), HASTA.plusDays(7)));
    }

    @Test
    void soloElDuenoProgramaPromociones() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), 20, DESDE, HASTA));
    }

    @Test
    void productoEliminadoNoTienePromociones() {
        producto.eliminar(false);

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), vendedorId, 20, DESDE, HASTA));
    }

    @Test
    void productoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, 20, DESDE, HASTA));
    }
}
