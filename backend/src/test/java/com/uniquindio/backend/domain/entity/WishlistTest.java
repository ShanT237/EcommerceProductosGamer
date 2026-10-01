package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WishlistTest {

    private UUID id;
    private UUID usuarioId;
    private Wishlist wishlist;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        usuarioId = UUID.randomUUID();
        wishlist = new Wishlist(id, usuarioId);
    }

    @Test
    void deberiaCrearWishlistVacia() {
        assertEquals(id, wishlist.getId());
        assertEquals(usuarioId, wishlist.getUsuarioId());
        assertTrue(wishlist.getProductos().isEmpty());
    }

    @Test
    void deberiaLanzarExcepcionSiIdEsNulo() {
        assertThrows(NullPointerException.class, () -> new Wishlist(null, usuarioId));
    }

    @Test
    void deberiaLanzarExcepcionSiUsuarioIdEsNulo() {
        assertThrows(NullPointerException.class, () -> new Wishlist(id, null));
    }

    @Test
    void deberiaAgregarProductoCorrectamente() {
        UUID productoId = UUID.randomUUID();

        wishlist.agregarProducto(productoId);

        assertTrue(wishlist.getProductos().contains(productoId));
        assertEquals(1, wishlist.getProductos().size());
    }

    @Test
    void deberiaLanzarExcepcionAlAgregarProductoNulo() {
        assertThrows(NullPointerException.class, () -> wishlist.agregarProducto(null));
    }

    @Test
    void deberiaLanzarExcepcionAlAgregarProductoDuplicado() {
        UUID productoId = UUID.randomUUID();
        wishlist.agregarProducto(productoId);

        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> wishlist.agregarProducto(productoId)
        );
        assertEquals("El producto ya se encuentra en la wishlist", excepcion.getMessage());
        assertEquals(1, wishlist.getProductos().size());
    }

    @Test
    void deberiaQuitarProductoCorrectamente() {
        UUID productoId = UUID.randomUUID();
        wishlist.agregarProducto(productoId);

        wishlist.quitarProducto(productoId);

        assertFalse(wishlist.getProductos().contains(productoId));
        assertTrue(wishlist.getProductos().isEmpty());
    }

    @Test
    void deberiaLanzarExcepcionAlQuitarProductoQueNoExiste() {
        UUID productoId = UUID.randomUUID();

        ReglaDominioException excepcion = assertThrows(
                ReglaDominioException.class,
                () -> wishlist.quitarProducto(productoId)
        );
        assertEquals("El producto no está en la wishlist", excepcion.getMessage());
    }

    @Test
    void listaDeProductosDevueltaDebeSerInmutable() {
        UUID productoId = UUID.randomUUID();
        wishlist.agregarProducto(productoId);

        assertThrows(UnsupportedOperationException.class,
                () -> wishlist.getProductos().add(UUID.randomUUID()));
    }

    @Test
    void dosWishlistsConMismoIdDebenSerIguales() {
        Wishlist otra = new Wishlist(id, UUID.randomUUID());

        assertEquals(wishlist, otra);
        assertEquals(wishlist.hashCode(), otra.hashCode());
    }

    @Test
    void dosWishlistsConDistintoIdNoDebenSerIguales() {
        Wishlist otra = new Wishlist(UUID.randomUUID(), usuarioId);

        assertNotEquals(wishlist, otra);
    }

    @Test
    void wishlistNoDebeSerIgualANuloNiAOtroTipo() {
        assertNotEquals(null, wishlist);
        assertNotEquals("no es una wishlist", wishlist);
    }
}