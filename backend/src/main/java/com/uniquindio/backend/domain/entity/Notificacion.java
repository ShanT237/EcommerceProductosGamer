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

    public static Notificacion porCambioEnWishlist(UUID usuarioId, Producto producto,
                                                   TipoNotificacion tipo, String detalle) {
        String mensaje = String.format("El producto '%s' en tu lista de deseos ha cambiado: %s",
                producto.getNombre(), detalle);
        return new Notificacion(UUID.randomUUID(), usuarioId, producto.getId(), tipo, mensaje, LocalDateTime.now());
    }

    public Notificacion(UUID id, UUID usuarioId, UUID productoId, TipoNotificacion tipo, String mensaje, LocalDateTime fecha) {
        this.id = Objects.requireNonNull(id, "El id de la notificación es obligatorio");
        this.usuarioId = Objects.requireNonNull(usuarioId, "El usuarioId es obligatorio");
        this.productoId = Objects.requireNonNull(productoId, "El productoId es obligatorio");
        this.tipo = Objects.requireNonNull(tipo, "El tipo de notificación es obligatorio");
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria");

        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaDominioException("El mensaje de la notificación no puede estar vacío");
        }

        this.mensaje = mensaje;
        this.leida = false;
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
