package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.CorreoNotificaciones;
import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: NotificarCambioWishlist (Regla 8).
 * Orquesta la generación y persistencia de notificaciones cuando un producto
 * de la lista de deseos cambia de precio o vuelve a tener stock.
 * Cada aviso de descuento también se manda por correo al comprador.
 */
@Service
public class NotificarDescuentoWishlistUseCase {

    private final WishlistRepository wishlistRepository;
    private final ProductoRepository productoRepository;
    private final NotificacionRepository notificacionRepository;
    private final CorreoNotificaciones correo;

    @Autowired
    public NotificarDescuentoWishlistUseCase(WishlistRepository wishlistRepository,
                                             ProductoRepository productoRepository,
                                             NotificacionRepository notificacionRepository,
                                             CorreoNotificaciones correo) {
        this.wishlistRepository = wishlistRepository;
        this.productoRepository = productoRepository;
        this.notificacionRepository = notificacionRepository;
        this.correo = correo;
    }

    /** Variante sin correo: solo persiste los avisos internos. */
    public NotificarDescuentoWishlistUseCase(WishlistRepository wishlistRepository,
                                             ProductoRepository productoRepository,
                                             NotificacionRepository notificacionRepository) {
        this(wishlistRepository, productoRepository, notificacionRepository, CorreoNotificaciones.sinEnvio());
    }

    public List<Notificacion> ejecutar(UUID productoId, double precioAnterior) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        producto.asegurarPrecioReducidoDesde(precioAnterior);

        List<Notificacion> notificaciones = wishlistRepository.obtenerTodasQueContengan(productoId).stream()
                .map(w -> Notificacion.porDescuentoEnWishlist(
                        w.getUsuarioId(), producto, precioAnterior))
                .toList();

        notificaciones.forEach(n -> {
            notificacionRepository.guardar(n);
            correo.enviar(n);
        });
        return notificaciones;
    }
}