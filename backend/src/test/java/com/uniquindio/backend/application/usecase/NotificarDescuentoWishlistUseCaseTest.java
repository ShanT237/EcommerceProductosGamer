package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NotificarDescuentoWishlistUseCaseTest {

    private WishlistRepository wishlistRepository;
    private ProductoRepository productoRepository;
    private NotificacionRepository notificacionRepository;
    private NotificarDescuentoWishlistUseCase useCase;
    private Producto producto;

    @BeforeEach
    void setUp() {
        wishlistRepository = new WishlistRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        useCase = new NotificarDescuentoWishlistUseCase(wishlistRepository, productoRepository, notificacionRepository);

        producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Teclado RGB", "Mecánico",
                120.0, 5, Gama.ALTA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
    }

    private UUID usuarioConWishlist(boolean conProducto) {
        UUID usuarioId = UUID.randomUUID();
        Wishlist w = Wishlist.crear(UUID.randomUUID(), usuarioId);
        if (conProducto) w.agregarProducto(producto.getId());
        wishlistRepository.guardar(w);
        return usuarioId;
    }

    @Test
    @DisplayName("Regla 8: notifica el descuento solo a quienes tienen el producto en su wishlist")
    void notificaSoloALosQueTienenElProducto() {
        UUID user1 = usuarioConWishlist(true);
        UUID user2 = usuarioConWishlist(true);
        UUID sinProducto = usuarioConWishlist(false);

        producto.aplicarDescuento(20);
        List<Notificacion> notificaciones = useCase.ejecutar(producto.getId(), 120.0);

        assertEquals(2, notificaciones.size());
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(user1).size());
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(user2).size());
        assertTrue(notificacionRepository.obtenerPorUsuarioId(sinProducto).isEmpty());
    }

    @Test
    @DisplayName("Regla 8: no se notifica si el precio no bajó")
    void rechazaSiNoHayDescuento() {
        usuarioConWishlist(true);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(producto.getId(), 120.0));
    }

    @Test
    @DisplayName("AplicarDescuento baja el precio y notifica a las wishlists")
    void aplicarDescuentoNotifica() {
        UUID user = usuarioConWishlist(true);
        AplicarDescuentoUseCase aplicar = new AplicarDescuentoUseCase(productoRepository, useCase);

        aplicar.ejecutar(producto.getId(), producto.getVendedorId(), 25);

        assertEquals(90.0, productoRepository.obtenerPorId(producto.getId()).get().getPrecio(), 0.001);
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(user).size());
    }
}
