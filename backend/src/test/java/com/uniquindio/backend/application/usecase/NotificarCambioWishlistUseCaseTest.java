package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
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

class NotificarCambioWishlistUseCaseTest {

    private WishlistRepository wishlistRepository;
    private ProductoRepository productoRepository;
    private NotificacionRepository notificacionRepository;
    private NotificarCambioWishlistUseCase useCase;

    @BeforeEach
    void setUp() {
        wishlistRepository = new WishlistRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        useCase = new NotificarCambioWishlistUseCase(wishlistRepository, productoRepository, notificacionRepository);
    }

    @Test
    @DisplayName("Regla 8: Notifica a los usuarios cuando un producto de su wishlist cambia de precio o stock")
    void notificaAUsuariosConProductoEnWishlist() {
        UUID vendedorId = UUID.randomUUID();
        Producto producto = new Producto(UUID.randomUUID(), vendedorId, "Teclado RGB", "Mecánico", 120.0, 5, Gama.ALTA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);

        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();

        Wishlist w1 = new Wishlist(UUID.randomUUID(), user1);
        w1.agregarProducto(producto.getId());
        wishlistRepository.guardar(w1);

        Wishlist w2 = new Wishlist(UUID.randomUUID(), user2);
        w2.agregarProducto(producto.getId());
        wishlistRepository.guardar(w2);

        List<Notificacion> notificaciones = useCase.ejecutar(
                producto.getId(),
                TipoNotificacion.CAMBIO_PRECIO,
                "Precio reducido a $99.99"
        );

        assertEquals(2, notificaciones.size());
        assertEquals(2, notificacionRepository.obtenerPorUsuarioId(user1).size() + notificacionRepository.obtenerPorUsuarioId(user2).size());
    }
}
