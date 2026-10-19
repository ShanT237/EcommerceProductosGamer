package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: QuitarDeWishlist (CU-15).
 * Quita un producto de la wishlist del usuario y persiste.
 */
@Service
@RequiredArgsConstructor
public class QuitarDeWishlistUseCase {

    private final WishlistRepository wishlistRepository;

    public Wishlist ejecutar(UUID usuarioId, UUID productoId) {
        Wishlist wishlist = wishlistRepository.obtenerPorUsuarioId(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("La wishlist no existe"));

        wishlist.quitarProducto(productoId);

        wishlistRepository.guardar(wishlist);

        return wishlist;
    }
}
