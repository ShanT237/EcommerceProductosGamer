package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: NotificarCambioWishlist (Regla 8).
 * Orquesta la generación y persistencia de notificaciones cuando un producto
 * de la lista de deseos cambia de precio o vuelve a tener stock.
 */
@Service
@RequiredArgsConstructor
public class NotificarDescuentoWishlistUseCase {

    private final WishlistRepository wishlistRepository;
    private final ProductoRepository productoRepository;
    private final NotificacionRepository notificacionRepository;

    public List<Notificacion> ejecutar(UUID productoId, double precioAnterior) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));

        if (producto.getPrecio() >= precioAnterior) {
            throw new ReglaDominioException("Solo se notifica cuando el producto tiene descuento");
        }

        String detalle = String.format("ahora cuesta %.2f (antes %.2f)",
                producto.getPrecio(), precioAnterior);

        List<Notificacion> notificaciones = wishlistRepository.obtenerTodasQueContengan(productoId).stream()
                .map(w -> Notificacion.porCambioEnWishlist(
                        w.getUsuarioId(), producto, TipoNotificacion.DESCUENTO, detalle))
                .toList();

        notificaciones.forEach(notificacionRepository::guardar);
        return notificaciones;
    }
}
