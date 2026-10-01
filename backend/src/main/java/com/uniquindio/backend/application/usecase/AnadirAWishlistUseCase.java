package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: AñadirAWishlist.
 * Orquesta la intención de guardar un producto en la wishlist: valida usuario y producto,
 * delega en Wishlist la regla de no duplicados y persiste.
 */
@Service
@RequiredArgsConstructor
public class AnadirAWishlistUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final WishlistRepository wishlistRepository;

    public Wishlist ejecutar(UUID wishlistId, UUID usuarioId, UUID productoId) {
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();

        Wishlist wishlist = wishlistRepository.obtenerPorUsuarioId(usuarioId)
                .orElse(new Wishlist(wishlistId, usuarioId));

        wishlist.agregarProducto(productoId);

        wishlistRepository.guardar(wishlist);

        return wishlist;
    }
}
