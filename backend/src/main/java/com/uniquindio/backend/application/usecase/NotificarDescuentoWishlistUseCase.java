package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
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
        producto.asegurarPrecioReducidoDesde(precioAnterior);

        List<Notificacion> notificaciones = wishlistRepository.obtenerTodasQueContengan(productoId).stream()
                .map(w -> Notificacion.porDescuentoEnWishlist(
                        w.getUsuarioId(), producto, precioAnterior))
                .toList();

        notificaciones.forEach(notificacionRepository::guardar);
        return notificaciones;
    }
}
