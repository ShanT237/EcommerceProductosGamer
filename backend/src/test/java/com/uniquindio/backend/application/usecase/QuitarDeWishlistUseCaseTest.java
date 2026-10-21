package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class QuitarDeWishlistUseCaseTest {

    private WishlistRepositoryEnMemoria wishlistRepository;
    private QuitarDeWishlistUseCase useCase;

    @BeforeEach
    void setUp() {
        wishlistRepository = new WishlistRepositoryEnMemoria();
        useCase = new QuitarDeWishlistUseCase(wishlistRepository);
    }

    @Test
    void quitaProductoYGuarda() {
        UUID usuarioId = UUID.randomUUID();
        UUID productoId = UUID.randomUUID();
        Wishlist guardada = Wishlist.crear(UUID.randomUUID(), usuarioId);
        guardada.agregarProducto(productoId);
        wishlistRepository.guardar(guardada);

        Wishlist w = useCase.ejecutar(usuarioId, productoId);

        assertTrue(w.getProductos().isEmpty());
        assertTrue(wishlistRepository.obtenerPorUsuarioId(usuarioId).get().getProductos().isEmpty());
    }

    @Test
    void sinWishlistLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), UUID.randomUUID()));
    }
}
