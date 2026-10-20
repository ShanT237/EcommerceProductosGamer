package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad MovimientoPuntos (CU-32): registro inmutable de puntos ganados o redimidos
 * por un usuario. Es el historial; el saldo vigente sigue viviendo en Usuario.
 */
@Getter
public class MovimientoPuntos {

    private final UUID id;
    private final UUID usuarioId;
    private final int cantidad;
    private final TipoMovimientoPuntos tipo;
    private final LocalDateTime fecha;

    private MovimientoPuntos(UUID id, UUID usuarioId, int cantidad, TipoMovimientoPuntos tipo,
                             LocalDateTime fecha) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.cantidad = cantidad;
        this.tipo = tipo;
        this.fecha = fecha;
    }

    public static MovimientoPuntos ganado(UUID usuarioId, int cantidad, LocalDateTime fecha) {
        return crear(usuarioId, cantidad, TipoMovimientoPuntos.GANADO, fecha);
    }

    public static MovimientoPuntos redimido(UUID usuarioId, int cantidad, LocalDateTime fecha) {
        return crear(usuarioId, cantidad, TipoMovimientoPuntos.REDIMIDO, fecha);
    }

    private static MovimientoPuntos crear(UUID usuarioId, int cantidad, TipoMovimientoPuntos tipo,
                                          LocalDateTime fecha) {
        Objects.requireNonNull(usuarioId, "El id del usuario es obligatorio");
        Objects.requireNonNull(fecha, "La fecha del movimiento es obligatoria");
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad de puntos del movimiento debe ser positiva");
        }
        return new MovimientoPuntos(UUID.randomUUID(), usuarioId, cantidad, tipo, fecha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovimientoPuntos that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
