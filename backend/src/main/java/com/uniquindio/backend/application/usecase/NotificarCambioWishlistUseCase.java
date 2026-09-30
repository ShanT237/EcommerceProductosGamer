package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: NotificarCambioWishlist (Regla 8).
 * Detecta cuando un producto de la lista de deseos cambia de precio o vuelve a tener stock,
 * y genera las notificaciones correspondientes a los usuarios interesados.
 */
@Service
public class NotificarCambioWishlistUseCase {

    private final WishlistRepository wishlistRepository;
    private final ProductoRepository productoRepository;
    private final NotificacionRepository notificacionRepository;

    public NotificarCambioWishlistUseCase(WishlistRepository wishlistRepository,
                                           ProductoRepository productoRepository,
                                           NotificacionRepository notificacionRepository) {
        this.wishlistRepository = wishlistRepository;
        this.productoRepository = productoRepository;
        this.notificacionRepository = notificacionRepository;
    }

    public List<Notificacion> ejecutar(UUID productoId, TipoNotificacion tipo, String detalle) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        List<Wishlist> wishlistsAfectadas = wishlistRepository.obtenerTodasQueContengan(productoId);
        List<Notificacion> notificacionesGeneradas = new ArrayList<>();

        for (Wishlist w : wishlistsAfectadas) {
            String mensaje = String.format("El producto '%s' en tu lista de deseos ha cambiado: %s",
                    producto.getNombre(), detalle);

            Notificacion notificacion = new Notificacion(
                    UUID.randomUUID(),
                    w.getUsuarioId(),
                    productoId,
                    tipo,
                    mensaje,
                    LocalDateTime.now()
            );

            notificacionRepository.guardar(notificacion);
            notificacionesGeneradas.add(notificacion);
        }

        return notificacionesGeneradas;
    }
}
