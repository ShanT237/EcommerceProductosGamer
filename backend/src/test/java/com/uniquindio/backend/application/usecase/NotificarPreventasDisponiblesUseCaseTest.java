package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificarPreventasDisponiblesUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    private PreventaRepositoryEnMemoria preventas;
    private ProductoRepositoryEnMemoria productos;
    private WishlistRepositoryEnMemoria wishlists;
    private NotificacionRepositoryEnMemoria notificaciones;
    private NotificarPreventasDisponiblesUseCase useCase;

    @BeforeEach
    void setUp() {
        preventas = new PreventaRepositoryEnMemoria();
        productos = new ProductoRepositoryEnMemoria();
        wishlists = new WishlistRepositoryEnMemoria();
        notificaciones = new NotificacionRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(8, 0).atZone(zona).toInstant(), zona);
        useCase = new NotificarPreventasDisponiblesUseCase(preventas, productos,
                TestNotificador.con(notificaciones, wishlists), reloj);
    }

    private Producto productoConPreventa(LocalDate lanzamiento) {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Consola X", "Edición limitada",
                900.0, 10, Gama.ALTA, Exclusividad.NORMAL);
        productos.guardar(producto);
        preventas.guardar(Preventa.reconstruir(UUID.randomUUID(), producto.getId(), lanzamiento, "Preventa"));
        return producto;
    }

    private UUID compradorConWishlist(Producto producto) {
        UUID usuarioId = UUID.randomUUID();
        Wishlist w = Wishlist.crear(UUID.randomUUID(), usuarioId);
        w.agregarProducto(producto.getId());
        wishlists.guardar(w);
        return usuarioId;
    }

    @Test
    void avisaALaWishlistElDiaDelLanzamientoYNoDuplicaAlRepetir() {
        Producto producto = productoConPreventa(HOY);
        UUID comprador = compradorConWishlist(producto);

        assertEquals(1, useCase.ejecutar());
        assertEquals(1, useCase.ejecutar());   // segunda corrida del día: no duplica

        var avisos = notificaciones.obtenerPorUsuarioId(comprador);
        assertEquals(1, avisos.size());
        assertEquals(TipoNotificacion.PREVENTA_DISPONIBLE, avisos.get(0).getTipo());
    }

    @Test
    void ignoraPreventasConOtraFechaDeLanzamiento() {
        Producto manana = productoConPreventa(HOY.plusDays(1));
        UUID comprador = compradorConWishlist(manana);

        assertEquals(0, useCase.ejecutar());
        assertTrue(notificaciones.obtenerPorUsuarioId(comprador).isEmpty());
    }
}
