package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Calificacion;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad: Reseña.
 * Tiene identidad propia (id), pasa por estados (sin respuesta -> respondida)
 * y está ligada a una compra concreta y completada.
 *
 * Sin setters: toda modificación de estado pasa por un método de negocio
 * con su propia validación (responder()).
 *
 */
public class Resena {

    private static final int MAX_CARACTERES_COMENTARIO = 500;

    private final String id;
    private final String idCompra;
    private final String idProducto;
    private final String idUsuario;
    private final Calificacion calificacion;
    private final String comentario;
    private final LocalDateTime fechaCreacion;

    private String respuestaVendedor;
    private LocalDateTime fechaRespuesta;

    private Resena(String id, String idCompra, String idProducto, String idUsuario,
                   Calificacion calificacion, String comentario, LocalDateTime fechaCreacion) {
        this.id = id;
        this.idCompra = idCompra;
        this.idProducto = idProducto;
        this.idUsuario = idUsuario;
        this.calificacion = calificacion;
        this.comentario = comentario;
        this.fechaCreacion = fechaCreacion;
    }

    /**
     * Factory de creación. Aplica las invariantes que SÍ dependen solo de
     * los datos de la propia reseña:
     * - Solo se puede reseñar una compra completada.
     * - El comentario es obligatorio y de máximo 500 caracteres.
     * - La calificación es obligatoria (1-5, validado por el VO Calificacion).
     */
    public static Resena crear(String id, String idCompra, String idProducto, String idUsuario,
                               boolean compraCompletada, Calificacion calificacion, String comentario) {
        if (!compraCompletada) {
            throw new ReglaDominioException(
                    "No se puede reseñar un producto sin una compra completada");
        }
        if (calificacion == null) {
            throw new ReglaDominioException("La reseña requiere una calificación");
        }
        if (comentario == null || comentario.isBlank()) {
            throw new ReglaDominioException("El comentario de la reseña es obligatorio");
        }
        if (comentario.length() > MAX_CARACTERES_COMENTARIO) {
            throw new ReglaDominioException(
                    "El comentario no puede superar " + MAX_CARACTERES_COMENTARIO + " caracteres");
        }
        return new Resena(id, idCompra, idProducto, idUsuario, calificacion, comentario, LocalDateTime.now());
    }

    /**
     * El vendedor responde a la reseña. Solo se permite una vez definida
     * la respuesta (no hay setter directo sobre respuestaVendedor).
     */
    public void responder(String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new ReglaDominioException("La respuesta del vendedor no puede estar vacía");
        }
        if (tieneRespuesta()) {
            throw new ReglaDominioException("Esta reseña ya tiene una respuesta del vendedor");
        }
        this.respuestaVendedor = respuesta;
        this.fechaRespuesta = LocalDateTime.now();
    }

    public boolean tieneRespuesta() {
        return respuestaVendedor != null;
    }

    public String getId() {
        return id;
    }

    public String getIdCompra() {
        return idCompra;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public Calificacion getCalificacion() {
        return calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public String getRespuestaVendedor() {
        return respuestaVendedor;
    }

    public LocalDateTime getFechaRespuesta() {
        return fechaRespuesta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Resena resena)) return false;
        return Objects.equals(id, resena.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}