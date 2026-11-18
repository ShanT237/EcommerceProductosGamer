package com.uniquindio.backend.application.service;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Punto único donde los casos de uso avisan de eventos del marketplace.
 * Cada método decide si el evento merece notificación (p. ej. solo cuando el stock CRUZA el
 * umbral, no en cada venta) y la persiste para el destinatario correcto.
 * Se llama dentro de la misma transacción del caso de uso: si este falla, no queda ningún aviso.
 * Además de persistir la notificación, pide a {@link CorreoNotificaciones} que la mande por correo
 * (solo los tipos que tienen plantilla; el resto queda únicamente dentro de la app).
 */
@Service
public class NotificadorEventos {

    private final NotificacionRepository notificacionRepository;
    private final WishlistRepository wishlistRepository;
    private final CorreoNotificaciones correo;

    @Autowired
    public NotificadorEventos(NotificacionRepository notificacionRepository,
                              WishlistRepository wishlistRepository,
                              CorreoNotificaciones correo) {
        this.notificacionRepository = notificacionRepository;
        this.wishlistRepository = wishlistRepository;
        this.correo = correo;
    }

    /** Notificador sin correo: solo persiste los avisos internos. */
    public NotificadorEventos(NotificacionRepository notificacionRepository,
                              WishlistRepository wishlistRepository) {
        this(notificacionRepository, wishlistRepository, CorreoNotificaciones.sinEnvio());
    }

    /** Guarda la notificación y, si su tipo lo admite, la manda también por correo. */
    private void publicar(Notificacion notificacion) {
        notificacionRepository.guardar(notificacion);
        correo.enviar(notificacion);
    }

    // ------------------------------------------------------------------ COMPRADOR

    /** Producto de la wishlist que vuelve a tener stock (estaba agotado y ahora hay unidades). */
    public void stockRepuesto(Producto producto, int stockAnterior) {
        if (stockAnterior > 0 || producto.getStock() <= 0 || producto.isEliminado()) {
            return;
        }
        String detalle = String.format("volvió a tener stock (%d disponibles)", producto.getStock());
        wishlistRepository.obtenerTodasQueContengan(producto.getId()).forEach(w ->
                publicar(Notificacion.porCambioEnWishlist(
                        w.getUsuarioId(), producto, TipoNotificacion.STOCK_DISPONIBLE, detalle)));
    }

    /** Preventa lanzada de un producto de la wishlist; un solo aviso por usuario y producto. */
    public void preventaDisponible(Producto producto, Preventa preventa) {
        wishlistRepository.obtenerTodasQueContengan(producto.getId()).forEach(w -> {
            if (notificacionRepository.existe(w.getUsuarioId(), producto.getId(), TipoNotificacion.PREVENTA_DISPONIBLE)) {
                return;
            }
            publicar(Notificacion.porCambioEnWishlist(
                    w.getUsuarioId(), producto, TipoNotificacion.PREVENTA_DISPONIBLE,
                    "su preventa ya está disponible, ¡puedes comprarlo!"));
        });
    }

    /** Se avisa cuando las compras completadas cruzan un múltiplo de COMPRAS_POR_REGALO. */
    public void metaAlcanzada(UUID usuarioId, long completadasAntes, long completadasDespues) {
        int meta = Usuario.COMPRAS_POR_REGALO;
        if (completadasDespues / meta <= completadasAntes / meta) {
            return;
        }
        publicar(Notificacion.crear(usuarioId, null, TipoNotificacion.META_ALCANZADA,
                String.format("¡Alcanzaste la meta de %d compras completadas! Ya puedes elegir tu regalo.",
                        (completadasDespues / meta) * meta)));
    }

    public void regaloEnviado(Regalo regalo) {
        if (regalo.getIdUsuario() == null) {
            return;
        }
        publicar(Notificacion.crear(regalo.getIdUsuario(), regalo.getIdProducto(),
                TipoNotificacion.REGALO_ENVIADO,
                "Tu regalo fue enviado. Confirma la entrega cuando lo recibas."));
    }

    /** El vendedor canceló un regalo ya elegido o enviado: se avisa al comprador (sin destinatario no hay aviso). */
    public void regaloCancelado(Regalo regalo, String nombre) {
        if (regalo.getIdUsuario() == null) {
            return;
        }
        publicar(Notificacion.crear(regalo.getIdUsuario(), regalo.getIdProducto(),
                TipoNotificacion.REGALO_CANCELADO,
                String.format("El vendedor canceló tu regalo '%s'. Recuperaste tu cupo: ya puedes elegir otro.",
                        nombre)));
    }

    // ------------------------------------------------------------------ VENDEDOR

    /** Un comprador eligió un regalo del vendedor: ya puede enviarlo. */
    public void regaloElegido(UUID vendedorId, Regalo regalo, String nombre) {
        publicar(Notificacion.crear(vendedorId, regalo.getIdProducto(),
                TipoNotificacion.REGALO_ELEGIDO,
                String.format("Un comprador eligió tu regalo '%s'. Ya puedes enviarlo.", nombre)));
    }

    /** Avisa al vendedor cuando el stock se agota o cruza hacia abajo el umbral de stock bajo. */
    public void stockDelVendedor(Producto producto, int stockAnterior) {
        int stock = producto.getStock();
        if (stock == 0 && stockAnterior > 0) {
            publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                    TipoNotificacion.STOCK_AGOTADO,
                    String.format("Tu producto '%s' se agotó.", producto.getNombre())));
        } else if (stock > 0 && stock <= Producto.UMBRAL_STOCK_BAJO && stockAnterior > Producto.UMBRAL_STOCK_BAJO) {
            publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                    TipoNotificacion.STOCK_BAJO,
                    String.format("Stock bajo: a tu producto '%s' le quedan %d unidades.",
                            producto.getNombre(), stock)));
        }
    }

    public void nuevaVenta(Producto producto, int cantidad) {
        publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                TipoNotificacion.NUEVA_VENTA,
                String.format("Nueva venta: %d %s de '%s'.", cantidad,
                        cantidad == 1 ? "unidad" : "unidades", producto.getNombre())));
    }

    public void reembolsoSolicitado(Producto producto, int cantidad) {
        publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                TipoNotificacion.REEMBOLSO_SOLICITADO,
                String.format("Reembolso solicitado: %d %s de '%s'.", cantidad,
                        cantidad == 1 ? "unidad" : "unidades", producto.getNombre())));
    }

    public void nuevaResena(Producto producto, int estrellas) {
        publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                TipoNotificacion.NUEVA_RESENA,
                String.format("Nueva reseña de %d %s en '%s'.", estrellas,
                        estrellas == 1 ? "estrella" : "estrellas", producto.getNombre())));
    }

    public void nuevaValoracion(Producto producto, int estrellas) {
        publicar(Notificacion.crear(producto.getVendedorId(), producto.getId(),
                TipoNotificacion.NUEVA_VALORACION,
                String.format("Un comprador te valoró con %d %s por '%s'.", estrellas,
                        estrellas == 1 ? "estrella" : "estrellas", producto.getNombre())));
    }
}