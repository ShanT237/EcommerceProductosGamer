package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Regalo.
 *  - Identidad: cada regalo tiene un id único.
 *  - Ciclo de vida: se crea disponible, se entrega a un usuario al completar una meta, no se recrea.
 *  - Regla 10: un usuario que complete una meta de compras podrá ser seleccionado para recibir un regalo.
 */
public class Regalo {

    private final UUID id;
    private String descripcion;
    private final int puntosRequeridos;
    private boolean entregado;
    private UUID usuarioId;

    public Regalo(UUID id, String descripcion, int puntosRequeridos) {
        this.id = Objects.requireNonNull(id, "El id del regalo es obligatorio");

        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción del regalo no puede estar vacía");
        }
        if (puntosRequeridos <= 0) {
            throw new ReglaDominioException("Los puntos requeridos para el regalo deben ser positivos");
        }

        this.descripcion = descripcion;
        this.puntosRequeridos = puntosRequeridos;
        this.entregado = false;
        this.usuarioId = null;
    }

    public UUID getId() {
        return id;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getPuntosRequeridos() {
        return puntosRequeridos;
    }

    public boolean isEntregado() {
        return entregado;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Regla del negocio: el regalo solo puede entregarse si el usuario cumple
     * la meta de puntos y el regalo no ha sido entregado previamente.
     */
    public void entregar(UUID usuarioId, int puntosUsuario) {
        Objects.requireNonNull(usuarioId, "El id del usuario es obligatorio para entregar el regalo");

        if (this.entregado) {
            throw new ReglaDominioException("El regalo ya fue entregado");
        }
        if (puntosUsuario < this.puntosRequeridos) {
            throw new ReglaDominioException("El usuario no cumple con la meta de puntos requerida");
        }

        this.entregado = true;
        this.usuarioId = usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Regalo)) return false;
        Regalo regalo = (Regalo) o;
        return id.equals(regalo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
