package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarWishlistUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private WishlistRepositoryEnMemoria wishlistRepository;
    private ConsultarWishlistUseCase useCase;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        wishlistRepository = new WishlistRepositoryEnMemoria();
        useCase = new ConsultarWishlistUseCase(wishlistRepository, productoRepository);
        usuarioId = UUID.randomUUID();
    }

    private Producto producto(String nombre) {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private void wishlistCon(Producto... productos) {
        Wishlist wishlist = Wishlist.crear(UUID.randomUUID(), usuarioId);
        for (Producto p : productos) {
            wishlist.agregarProducto(p.getId());
        }
        wishlistRepository.guardar(wishlist);
    }

    @Test
    void devuelveLosProductosVisiblesEnElOrdenDeLaWishlist() {
        Producto mouse = producto("Mouse");
        Producto teclado = producto("Teclado");
        wishlistCon(teclado, mouse);

        List<UUID> ids = useCase.ejecutar(usuarioId).stream().map(Producto::getId).toList();

        assertEquals(List.of(teclado.getId(), mouse.getId()), ids);
    }

    @Test
    void usuarioSinWishlistDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(usuarioId).isEmpty());
    }

    @Test
    void ignoraProductosEliminadosOPausados() {
        Producto visible = producto("Visible");
        Producto eliminado = producto("Eliminado");
        Producto pausado = producto("Pausado");
        wishlistCon(eliminado, pausado, visible);
        eliminado.eliminar(false);
        pausado.pausar();

        List<UUID> ids = useCase.ejecutar(usuarioId).stream().map(Producto::getId).toList();

        assertEquals(List.of(visible.getId()), ids);
    }

    @Test
    void ignoraProductosQueYaNoExisten() {
        Producto existente = producto("Existe");
        Wishlist wishlist = Wishlist.crear(UUID.randomUUID(), usuarioId);
        wishlist.agregarProducto(UUID.randomUUID());
        wishlist.agregarProducto(existente.getId());
        wishlistRepository.guardar(wishlist);

        List<UUID> ids = useCase.ejecutar(usuarioId).stream().map(Producto::getId).toList();

        assertEquals(List.of(existente.getId()), ids);
    }
}
