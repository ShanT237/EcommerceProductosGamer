package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProgramarPreventaUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private VendedorRepositoryEnMemoria vendedorRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private ProgramarPreventaUseCase useCase;
    private Vendedor vendedor;
    private Producto producto;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
        useCase = new ProgramarPreventaUseCase(
                vendedorRepository, productoRepository, preventaRepository, reloj);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "a@b.com",
                TipoVendedor.EMPRESA, "900123");
        vendedorRepository.guardar(vendedor);
        producto = Producto.publicar(UUID.randomUUID(), vendedor.getId(), "Teclado", "Desc",
                100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    @Test
    @DisplayName("Programa la preventa y la guarda")
    void casoFeliz() {
        LocalDate lanzamiento = HOY.plusDays(10);

        Preventa preventa = useCase.ejecutar(vendedor.getId(), producto.getId(),
                lanzamiento, "Edición coleccionista");

        assertEquals(producto.getId(), preventa.getProductoId());
        assertEquals(lanzamiento, preventa.getFechaLanzamiento());
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isPresent());
    }

    @Test
    @DisplayName("Rechaza una fecha de lanzamiento pasada o igual a hoy")
    void fechaPasada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), producto.getId(), HOY.minusDays(1), "Ayer"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), producto.getId(), HOY, "Hoy"));
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isEmpty());
    }

    @Test
    @DisplayName("Rechaza una segunda preventa del mismo producto")
    void preventaDuplicada() {
        useCase.ejecutar(vendedor.getId(), producto.getId(), HOY.plusDays(5), "Primera");

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(vendedor.getId(), producto.getId(), HOY.plusDays(20), "Segunda"));

        assertEquals("El producto ya tiene una preventa programada", ex.getMessage());
    }

    @Test
    @DisplayName("Rechaza un producto que no pertenece al vendedor")
    void productoDeOtroVendedor() {
        Producto ajeno = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Ajeno", "Desc",
                50.0, 5, Gama.BAJA, Exclusividad.NORMAL);
        productoRepository.guardar(ajeno);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), ajeno.getId(), HOY.plusDays(5), "Preventa"));
        assertTrue(preventaRepository.obtenerPorProductoId(ajeno.getId()).isEmpty());
    }

    @Test
    @DisplayName("Rechaza un producto eliminado")
    void productoEliminado() {
        producto.eliminar(false);
        productoRepository.guardar(producto);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), producto.getId(), HOY.plusDays(5), "Preventa"));
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isEmpty());
    }

    @Test
    @DisplayName("Rechaza un vendedor dado de baja")
    void vendedorDadoDeBaja() {
        vendedor.darDeBaja();
        vendedorRepository.guardar(vendedor);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), producto.getId(), HOY.plusDays(5), "Preventa"));
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isEmpty());
    }

    @Test
    @DisplayName("Vendedor o producto inexistente lanza NoSuchElementException")
    void inexistentes() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(
                UUID.randomUUID(), producto.getId(), HOY.plusDays(5), "Preventa"));
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(
                vendedor.getId(), UUID.randomUUID(), HOY.plusDays(5), "Preventa"));
    }

    @Test
    @DisplayName("Rechaza una fecha de lanzamiento nula")
    void fechaNula() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                vendedor.getId(), producto.getId(), null, "Preventa"));
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isEmpty());
    }
}
