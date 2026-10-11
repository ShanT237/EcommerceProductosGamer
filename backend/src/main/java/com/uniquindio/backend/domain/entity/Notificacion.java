package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Notificacion (Regla 8).
 * Representa una notificación generada para un usuario cuando un producto de su
 * wishlist cambia de precio o vuelve a tener stock.
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

    public void marcarComoLeida() {
        this.leida = true;
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
