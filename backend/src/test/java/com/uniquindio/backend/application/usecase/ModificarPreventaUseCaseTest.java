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

class ModificarPreventaUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);
    private static final ZoneId ZONA = ZoneId.systemDefault();

    private VendedorRepositoryEnMemoria vendedorRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private ModificarPreventaUseCase useCase;
    private Vendedor vendedor;
    private Producto producto;
    private Preventa preventa;

    @BeforeEach
    void setUp() {
        vendedorRepository = new VendedorRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(ZONA).toInstant(), ZONA);
        useCase = new ModificarPreventaUseCase(vendedorRepository, productoRepository, preventaRepository, reloj);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "a@b.com",
                TipoVendedor.EMPRESA, "900123");
        vendedorRepository.guardar(vendedor);
        producto = Producto.publicar(UUID.randomUUID(), vendedor.getId(), "Teclado", "Desc",
                100.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        preventa = Preventa.programar(UUID.randomUUID(), producto.getId(), HOY.plusDays(10), "Original", HOY);
        preventaRepository.guardar(preventa);
    }

    @Test
    @DisplayName("Modifica fecha y descripción y persiste el cambio")
    void casoFeliz() {
        LocalDate nueva = HOY.plusDays(40);

        Preventa modificada = useCase.ejecutar(preventa.getId(), vendedor.getId(), nueva, "Edición ampliada");

        assertEquals(preventa.getId(), modificada.getId());
        assertEquals(producto.getId(), modificada.getProductoId());
        Preventa guardada = preventaRepository.obtenerPorId(preventa.getId()).orElseThrow();
        assertEquals(nueva, guardada.getFechaLanzamiento());
        assertEquals("Edición ampliada", guardada.getDescripcion());
    }

    @Test
    @DisplayName("Rechaza una fecha pasada o igual a hoy y deja la preventa intacta")
    void fechaNoFutura() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), vendedor.getId(), HOY, "Hoy"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), vendedor.getId(), HOY.minusDays(1), "Ayer"));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), vendedor.getId(), null, "Sin fecha"));

        Preventa guardada = preventaRepository.obtenerPorId(preventa.getId()).orElseThrow();
        assertEquals(HOY.plusDays(10), guardada.getFechaLanzamiento());
        assertEquals("Original", guardada.getDescripcion());
    }

    @Test
    @DisplayName("Rechaza una descripción vacía")
    void descripcionVacia() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), vendedor.getId(), HOY.plusDays(20), "  "));
        assertEquals("Original", preventaRepository.obtenerPorId(preventa.getId()).orElseThrow().getDescripcion());
    }

    @Test
    @DisplayName("Rechaza modificar una preventa que ya fue lanzada")
    void preventaYaLanzada() {
        Preventa lanzada = Preventa.reconstruir(UUID.randomUUID(), producto.getId(), HOY, "Lanzada");
        preventaRepository.guardar(lanzada);

        ReglaDominioException ex = assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                lanzada.getId(), vendedor.getId(), HOY.plusDays(20), "Nueva"));

        assertEquals("La preventa ya fue lanzada y no se puede modificar", ex.getMessage());
    }

    @Test
    @DisplayName("Rechaza a un vendedor que no es dueño del producto")
    void vendedorAjeno() {
        Vendedor otro = Vendedor.registrar(UUID.randomUUID(), "Otra Tienda", "o@b.com",
                TipoVendedor.EMPRESA, "900124");
        vendedorRepository.guardar(otro);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), otro.getId(), HOY.plusDays(20), "Intento ajeno"));
        assertEquals("Original", preventaRepository.obtenerPorId(preventa.getId()).orElseThrow().getDescripcion());
    }

    @Test
    @DisplayName("Rechaza un producto eliminado y un vendedor dado de baja")
    void productoEliminadoOVendedorDeBaja() {
        vendedor.darDeBaja();
        vendedorRepository.guardar(vendedor);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                preventa.getId(), vendedor.getId(), HOY.plusDays(20), "Baja"));

        Vendedor activo = Vendedor.registrar(UUID.randomUUID(), "Activo", "act@b.com",
                TipoVendedor.EMPRESA, "900125");
        vendedorRepository.guardar(activo);
        Producto propio = Producto.publicar(UUID.randomUUID(), activo.getId(), "Mouse", "Desc",
                50.0, 5, Gama.BAJA, Exclusividad.NORMAL);
        Preventa otra = Preventa.programar(UUID.randomUUID(), propio.getId(), HOY.plusDays(3), "Otra", HOY);
        propio.eliminar(false);
        productoRepository.guardar(propio);
        preventaRepository.guardar(otra);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(
                otra.getId(), activo.getId(), HOY.plusDays(20), "Eliminado"));
    }

    @Test
    @DisplayName("Preventa o vendedor inexistente lanza NoSuchElementException")
    void inexistentes() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(
                UUID.randomUUID(), vendedor.getId(), HOY.plusDays(20), "No existe"));
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(
                preventa.getId(), UUID.randomUUID(), HOY.plusDays(20), "Vendedor fantasma"));
    }
}
