package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BuscarProductosUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private ProductoRepositoryEnMemoria productoRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private BuscarProductosUseCase useCase;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new BuscarProductosUseCase(productoRepository, preventaRepository, reloj);
    }

    private Producto guardar(String nombre, String categoria, Gama gama, double precio) {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc",
                precio, 5, gama, Exclusividad.NORMAL, categoria);
        productoRepository.guardar(p);
        return p;
    }

    @Test
    void filtroVacioDevuelveTodosLosVisibles() {
        guardar("Mouse", "Perifericos", Gama.MEDIA, 50);
        guardar("Silla", "Mobiliario", Gama.ALTA, 300);

        assertEquals(2, useCase.ejecutar(FiltroProducto.sinFiltros()).size());
    }

    @Test
    void excluyeEliminadosYPausados() {
        Producto visible = guardar("Mouse", "Perifericos", Gama.MEDIA, 50);
        guardar("Eliminado", "Perifericos", Gama.MEDIA, 50).eliminar(false);
        guardar("Pausado", "Perifericos", Gama.MEDIA, 50).pausar();

        assertEquals(List.of(visible), useCase.ejecutar(FiltroProducto.sinFiltros()));
    }

    @Test
    void filtraPorCategoriaYPrecio() {
        Producto barato = guardar("Mouse", "Perifericos", Gama.BAJA, 30);
        guardar("Teclado", "Perifericos", Gama.ALTA, 200);
        guardar("Silla", "Mobiliario", Gama.ALTA, 25);

        List<Producto> r = useCase.ejecutar(new FiltroProducto(null, "Perifericos", null, null, 100.0, false));

        assertEquals(List.of(barato), r);
    }

    @Test
    void soloPreventaDevuelveProductosCuyaFechaDeLanzamientoNoLlego() {
        Producto enPreventa = guardar("Consola", "Juegos", Gama.ALTA, 500);
        Producto yaLanzado = guardar("Juego", "Juegos", Gama.MEDIA, 60);
        guardar("Normal", "Juegos", Gama.BAJA, 10);
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), enPreventa.getId(),
                HOY.plusDays(10), "Lanzamiento"));
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), yaLanzado.getId(),
                HOY, "Ya salió"));

        List<Producto> r = useCase.ejecutar(new FiltroProducto(null, null, null, null, null, true));

        assertEquals(List.of(enPreventa), r);
    }

    @Test
    void sinSoloPreventaTambienListaLosProductosEnPreventa() {
        Producto enPreventa = guardar("Consola", "Juegos", Gama.ALTA, 500);
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), enPreventa.getId(),
                HOY.plusDays(10), "Lanzamiento"));

        assertEquals(List.of(enPreventa), useCase.ejecutar(FiltroProducto.sinFiltros()));
    }
}
