package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelarPreventaUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private ProductoRepositoryEnMemoria productoRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private CancelarPreventaUseCase useCase;
    private UUID vendedorId;
    private Producto producto;
    private Preventa preventa;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
        useCase = new CancelarPreventaUseCase(productoRepository, preventaRepository, reloj);

        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Teclado", "Desc",
                100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        preventa = Preventa.programar(UUID.randomUUID(), producto.getId(), HOY.plusDays(10), "Original", HOY);
        preventaRepository.guardar(preventa);
    }

    @Test
    @DisplayName("Cancela la preventa: desaparece y el producto queda libre de restricción por fecha")
    void casoFeliz() {
        useCase.ejecutar(preventa.getId(), vendedorId);

        assertTrue(preventaRepository.obtenerPorId(preventa.getId()).isEmpty());
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isEmpty());
        assertTrue(preventaRepository.obtenerFuturas(HOY).isEmpty());
    }

    @Test
    @DisplayName("Tras cancelar se puede programar una nueva preventa para el mismo producto")
    void permiteProgramarDeNuevo() {
        useCase.ejecutar(preventa.getId(), vendedorId);

        assertDoesNotThrow(() -> preventaRepository.guardar(
                Preventa.programar(UUID.randomUUID(), producto.getId(), HOY.plusDays(60), "Nueva", HOY)));
        assertTrue(preventaRepository.obtenerPorProductoId(producto.getId()).isPresent());
    }

    @Test
    @DisplayName("Rechaza cancelar una preventa que ya fue lanzada")
    void preventaYaLanzada() {
        Preventa lanzada = Preventa.reconstruir(UUID.randomUUID(), producto.getId(), HOY, "Lanzada");
        preventaRepository.guardar(lanzada);

        ReglaDominioException ex = assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(lanzada.getId(), vendedorId));

        assertEquals("La preventa ya fue lanzada y no se puede cancelar", ex.getMessage());
        assertTrue(preventaRepository.obtenerPorId(lanzada.getId()).isPresent());
    }

    @Test
    @DisplayName("Rechaza a un vendedor que no es dueño del producto")
    void vendedorAjeno() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(preventa.getId(), UUID.randomUUID()));
        assertTrue(preventaRepository.obtenerPorId(preventa.getId()).isPresent());
    }

    @Test
    @DisplayName("Preventa o producto inexistente lanza NoSuchElementException")
    void inexistentes() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId));

        Preventa huerfana = Preventa.programar(UUID.randomUUID(), UUID.randomUUID(), HOY.plusDays(4), "Huérfana", HOY);
        preventaRepository.guardar(huerfana);
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(huerfana.getId(), vendedorId));
    }
}
