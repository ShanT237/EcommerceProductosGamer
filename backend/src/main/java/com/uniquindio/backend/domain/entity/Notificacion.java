package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Notificacion (Regla 8).
 * Notificación dirigida a un comprador o a un vendedor (el "usuarioId" es el id del destinatario).
 * Compradores: descuento, stock repuesto o preventa lanzada de un producto de su wishlist,
 * meta de compras alcanzada, regalo enviado y regalo cancelado.
 * Vendedores: stock bajo/agotado, nueva venta, reembolso solicitado, nueva reseña, nueva valoración y regalo elegido.
 * El producto es opcional: hay avisos (p. ej. meta alcanzada) que no se refieren a uno.
 */
@Getter
public class Notificacion {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID productoId;
    private final TipoNotificacion tipo;
    private final String mensaje;
    private final LocalDateTime fecha;
    private boolean leida;

    private Notificacion(UUID id, UUID usuarioId, UUID productoId, TipoNotificacion tipo,
                         String mensaje, LocalDateTime fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.productoId = productoId;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.fecha = fecha;
        this.leida = false;
    }

    public static Notificacion porCambioEnWishlist(UUID usuarioId, Producto producto,
                                                   TipoNotificacion tipo, String detalle) {
        Objects.requireNonNull(usuarioId, "El usuarioId es obligatorio");
        Objects.requireNonNull(producto, "El producto es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de notificación es obligatorio");
        if (detalle == null || detalle.isBlank()) {
            throw new ReglaDominioException("El mensaje de la notificación no puede estar vacío");
        }

        String mensaje = String.format("El producto '%s' en tu lista de deseos ha cambiado: %s",
                producto.getNombre(), detalle);
        return new Notificacion(UUID.randomUUID(), usuarioId, producto.getId(), tipo,
                mensaje, LocalDateTime.now());
    }

    public static Notificacion porDescuentoEnWishlist(UUID usuarioId, Producto producto,
                                                      double precioAnterior) {
        Objects.requireNonNull(producto, "El producto es obligatorio");
        producto.asegurarPrecioReducidoDesde(precioAnterior);
        String detalle = String.format("ahora cuesta %.2f (antes %.2f)",
                producto.getPrecio(), precioAnterior);
        return porCambioEnWishlist(usuarioId, producto, TipoNotificacion.DESCUENTO, detalle);
    }

    /**
     * Notificación genérica para un destinatario (comprador o vendedor). El producto es opcional.
     */
    public static Notificacion crear(UUID destinatarioId, UUID productoId, TipoNotificacion tipo, String mensaje) {
        Objects.requireNonNull(destinatarioId, "El destinatario es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de notificación es obligatorio");
        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaDominioException("El mensaje de la notificación no puede estar vacío");
        }
        return new Notificacion(UUID.randomUUID(), destinatarioId, productoId, tipo, mensaje, LocalDateTime.now());
    }

    public static Notificacion reconstruir(UUID id, UUID usuarioId, UUID productoId,
                                           TipoNotificacion tipo, String mensaje,
                                           LocalDateTime fecha, boolean leida) {
        Notificacion n = new Notificacion(id, usuarioId, productoId, tipo, mensaje, fecha);
        n.leida = leida;
        return n;
    }

    public void marcarComoLeida() {
        this.leida = true;
    }

    /** Garantiza que la notificación pertenece al usuario indicado (Regla 8: solo el dueño la gestiona). */
    public void asegurarPerteneceA(UUID usuarioId) {
        if (!this.usuarioId.equals(usuarioId)) {
            throw new ReglaDominioException("La notificación no pertenece al usuario");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notificacion that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
