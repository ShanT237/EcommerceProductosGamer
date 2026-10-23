package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AplicarDescuentoUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private WishlistRepositoryEnMemoria wishlistRepository;
    private NotificacionRepositoryEnMemoria notificacionRepository;
    private AplicarDescuentoUseCase useCase;
    private UUID vendedorId;
    private UUID usuarioId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        wishlistRepository = new WishlistRepositoryEnMemoria();
        notificacionRepository = new NotificacionRepositoryEnMemoria();
        useCase = new AplicarDescuentoUseCase(productoRepository,
                new NotificarDescuentoWishlistUseCase(wishlistRepository, productoRepository, notificacionRepository));

        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Mouse", "Mouse RGB",
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);

        usuarioId = UUID.randomUUID();
        Wishlist wishlist = Wishlist.crear(UUID.randomUUID(), usuarioId);
        wishlist.agregarProducto(producto.getId());
        wishlistRepository.guardar(wishlist);
    }

    @Test
    @DisplayName("El vendedor dueño aplica el descuento y se notifica a la wishlist")
    void elDuenoAplicaElDescuento() {
        Producto resultado = useCase.ejecutar(producto.getId(), vendedorId, 20);

        assertEquals(80.0, resultado.getPrecio(), 0.001);
        assertEquals(80.0, productoRepository.obtenerPorId(producto.getId()).get().getPrecio(), 0.001);
        assertEquals(1, notificacionRepository.obtenerPorUsuarioId(usuarioId).size());
    }

    @Test
    @DisplayName("Un vendedor ajeno no puede aplicar el descuento: el precio y las notificaciones no cambian")
    void unVendedorAjenoNoAplicaElDescuento() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(producto.getId(), UUID.randomUUID(), 20));

        assertEquals(100.0, productoRepository.obtenerPorId(producto.getId()).get().getPrecio(), 0.001);
        assertTrue(notificacionRepository.obtenerPorUsuarioId(usuarioId).isEmpty());
    }

    @Test
    @DisplayName("Producto inexistente lanza NoSuchElementException")
    void productoInexistente() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedorId, 20));
    }
}