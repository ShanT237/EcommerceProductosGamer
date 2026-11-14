package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ConsultarWishlist.
 * Productos favoritos del usuario en el orden en que los añadió. Omite los que ya no existen y
 * los que no están visibles (eliminados o pausados). Sin wishlist devuelve una lista vacía.
 */
@Service
@RequiredArgsConstructor
public class ConsultarWishlistUseCase {

    private final WishlistRepository wishlistRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<Producto> ejecutar(UUID usuarioId) {
        return wishlistRepository.obtenerPorUsuarioId(usuarioId)
                .map(Wishlist::getProductos)
                .orElse(List.of())
                .stream()
                .map(productoRepository::obtenerPorId)
                .flatMap(Optional::stream)
                .filter(Producto::estaVisible)
                .toList();
    }
}
