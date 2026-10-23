package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CatalogoRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Pagina;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BuscarProductosUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    @Test
    void delegaFiltrosPaginacionYFechaAlRepositorioDeCatalogo() {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "Gamer",
                50, 5, Gama.MEDIA, Exclusividad.NORMAL, "Perifericos");
        FiltroProducto filtro = new FiltroProducto("mouse", "Perifericos", Gama.MEDIA,
                20.0, 100.0, true);
        AtomicReference<FiltroProducto> filtroRecibido = new AtomicReference<>();
        AtomicReference<String> parametrosRecibidos = new AtomicReference<>();
        CatalogoRepository catalogo = (criterio, pagina, tamano, hoy) -> {
            filtroRecibido.set(criterio);
            parametrosRecibidos.set(pagina + ":" + tamano + ":" + hoy);
            return new Pagina<>(List.of(producto), pagina, tamano, 1, 1);
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);
        BuscarProductosUseCase useCase = new BuscarProductosUseCase(catalogo, reloj);

        Pagina<Producto> resultado = useCase.ejecutar(filtro, 2, 10);

        assertEquals(filtro, filtroRecibido.get());
        assertEquals("2:10:" + HOY, parametrosRecibidos.get());
        assertEquals(List.of(producto), resultado.contenido());
    }

    @Test
    void rechazaPaginacionFueraDeRango() {
        BuscarProductosUseCase useCase = new BuscarProductosUseCase(
                (filtro, pagina, tamano, hoy) -> new Pagina<>(List.of(), pagina, tamano, 0, 0),
                Clock.systemUTC());

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(FiltroProducto.sinFiltros(), -1, 10));
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(FiltroProducto.sinFiltros(), 0, 101));
    }
}
