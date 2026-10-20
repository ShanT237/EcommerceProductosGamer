package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad ValoracionVendedor (CU-28): calificación del servicio de un vendedor, ligada a
 * una compra completada (a diferencia de Resena, que califica el producto).
 * Comentario opcional, máximo 500 caracteres. Inmutable.
 */
@Getter
public class ValoracionVendedor {

    private static final int MAX_CARACTERES_COMENTARIO = 500;

    private final UUID id;
    private final UUID compraId;
    private final UUID usuarioId;
    private final UUID vendedorId;
    private final Calificacion calificacion;
    private final String comentario;
    private final LocalDateTime fecha;

    private ValoracionVendedor(UUID id, UUID compraId, UUID usuarioId, UUID vendedorId,
                               Calificacion calificacion, String comentario, LocalDateTime fecha) {
        this.id = id;
        this.compraId = compraId;
        this.usuarioId = usuarioId;
        this.vendedorId = vendedorId;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fecha = fecha;
    }

    public static ValoracionVendedor crear(UUID id, UUID compraId, UUID usuarioId, UUID vendedorId,
                                           boolean compraCompletada, Calificacion calificacion,
                                           String comentario, LocalDateTime fecha) {
        Objects.requireNonNull(id, "El id de la valoración es obligatorio");
        Objects.requireNonNull(compraId, "La compra es obligatoria");
        Objects.requireNonNull(usuarioId, "El usuario es obligatorio");
        Objects.requireNonNull(vendedorId, "El vendedor es obligatorio");
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        if (!compraCompletada) {
            throw new ReglaDominioException("Solo se puede valorar al vendedor de una compra completada");
        }
        if (calificacion == null) {
            throw new ReglaDominioException("La valoración requiere una calificación");
        }
        String texto = comentario == null ? "" : comentario;
        if (texto.length() > MAX_CARACTERES_COMENTARIO) {
            throw new ReglaDominioException(
                    "El comentario no puede superar " + MAX_CARACTERES_COMENTARIO + " caracteres");
        }
        return new ValoracionVendedor(id, compraId, usuarioId, vendedorId, calificacion, texto, fecha);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValoracionVendedor that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
