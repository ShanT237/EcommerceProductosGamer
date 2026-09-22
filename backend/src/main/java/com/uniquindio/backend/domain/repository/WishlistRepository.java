package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Wishlist;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Wishlist.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar la wishlist de un usuario.
 */
public interface WishlistRepository {

    Optional<Wishlist> obtenerPorUsuarioId(UUID usuarioId);

    void guardar(Wishlist wishlist);
}
