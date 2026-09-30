package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Caso de uso: AñadirAWishlist.
 * Recibe la intención de un usuario de guardar un producto en su wishlist.
 * Verifica que el usuario y el producto existan (y no esté eliminado), obtiene la wishlist
 * del usuario y delega en ella la regla de no duplicados (Regla 7).
 */
@Service
public class AnadirAWishlistUseCase {

    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final WishlistRepository wishlistRepository;

    public AnadirAWishlistUseCase(UsuarioRepository usuarioRepository,
                                  ProductoRepository productoRepository,
                                  WishlistRepository wishlistRepository) {
        this.usuarioRepository = usuarioRepository;
        this.productoRepository = productoRepository;
        this.wishlistRepository = wishlistRepository;
    }

    public Wishlist ejecutar(UUID wishlistId, UUID usuarioId, UUID productoId) {
        // 1. Verificar que el usuario y el producto existen
        usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        if (producto.isEliminado()) {
            throw new ReglaDominioException("No se puede agregar a la wishlist un producto eliminado");
        }

        // 2. Obtener la wishlist del usuario, o crearla si aún no tiene una
        Wishlist wishlist = wishlistRepository.obtenerPorUsuarioId(usuarioId)
                .orElse(new Wishlist(wishlistId, usuarioId));

        // 3. El dominio aplica la Regla 7: no se puede agregar el mismo producto dos veces
        wishlist.agregarProducto(productoId);

        // 4. Persistir el nuevo estado
        wishlistRepository.guardar(wishlist);

        return wishlist;
    }
}
