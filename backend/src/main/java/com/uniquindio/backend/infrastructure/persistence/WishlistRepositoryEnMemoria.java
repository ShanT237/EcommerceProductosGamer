package com.uniquindio.backend.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.repository.WishlistRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class WishlistRepositoryEnMemoria implements WishlistRepository {

    private final Map<UUID, Wishlist> wishlists = new HashMap<>();

    @Override
    public Optional<Wishlist> obtenerPorUsuarioId(UUID usuarioId) {
        return wishlists.values().stream()
                .filter(w -> w.getUsuarioId().equals(usuarioId))
                .findFirst();
    }

    @Override
    public void guardar(Wishlist wishlist) {
        wishlists.put(wishlist.getId(), wishlist);
    }

    @Override
    public List<Wishlist> obtenerTodasQueContengan(UUID productoId) {
        List<Wishlist> resultado = new ArrayList<>();
        for (Wishlist w : wishlists.values()) {
            if (w.getProductos().contains(productoId)) {
                resultado.add(w);
            }
        }
        return resultado;
    }
}