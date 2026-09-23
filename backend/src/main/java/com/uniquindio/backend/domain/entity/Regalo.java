package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Regalo — raíz de agregado.
 *  - Identidad: dos regalos son el mismo solo si comparten el mismo id.
 *  - Ciclo de vida: disponible -> elegido -> enviado -> entregado,
 *    con posibilidad de cancelado en cualquier punto antes de entregado.
 *  - Controla EstadoRegalo (dentro del límite del agregado).
 *  - Usuario, Producto y Combo están FUERA del agregado: solo se referencian por id.
 */
public class Regalo {

    private final UUID id;
    private EstadoRegalo estado;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaEntrega;
    private final UUID idProducto;
    private UUID idUsuario;
    private final UUID idCombo;

    private Regalo(UUID id, UUID idProducto, UUID idCombo) {
        this.id = Objects.requireNonNull(id, "El id del regalo es obligatorio");
        this.idProducto = idProducto;
        this.idCombo = idCombo;
        this.estado = EstadoRegalo.DISPONIBLE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public static Regalo crear(UUID id, UUID idProducto, UUID idCombo) {
        if (idProducto == null && idCombo == null) {
            throw new ReglaDominioException("Un regalo debe estar asociado a un producto o a un combo");
        }
        return new Regalo(id, idProducto, idCombo);
    }

    public void elegir(UUID idUsuario, boolean usuarioYaPoseeElProducto) {
        Objects.requireNonNull(idUsuario, "El id del usuario es obligatorio para elegir el regalo");

        if (this.estado != EstadoRegalo.DISPONIBLE) {
            throw new ReglaDominioException("El regalo ya fue elegido, no se puede reasignar a otro usuario");
        }
        if (usuarioYaPoseeElProducto) {
            throw new ReglaDominioException("El regalo no puede elegirse a un usuario que ya posee ese producto");
        }

        this.idUsuario = idUsuario;
        this.estado = EstadoRegalo.ELEGIDO;
    }

    public void notificar(UUID idUsuario) {
        if (this.estado != EstadoRegalo.ELEGIDO) {
            throw new ReglaDominioException("Solo se notifica un regalo que ya fue elegido");
        }
        if (!this.idUsuario.equals(idUsuario)) {
            throw new ReglaDominioException("Solo se notifica al usuario al que se le eligió el regalo");
        }
    }

    public void enviar() {
        if (this.estado != EstadoRegalo.ELEGIDO) {
            throw new ReglaDominioException("Un regalo no puede enviarse si no ha sido elegido");
        }
        this.estado = EstadoRegalo.ENVIADO;
    }

    public void confirmarEntrega() {
        if (this.estado != EstadoRegalo.ENVIADO) {
            throw new ReglaDominioException("Un regalo no puede entregarse si no ha sido enviado");
        }
        this.estado = EstadoRegalo.ENTREGADO;
        this.fechaEntrega = LocalDateTime.now();
    }

    public void cancelarRegalo() {
        if (this.estado == EstadoRegalo.ENTREGADO) {
            throw new ReglaDominioException("Un regalo entregado no puede cambiar de estado");
        }
        if (this.estado == EstadoRegalo.CANCELADO) {
            throw new ReglaDominioException("El regalo ya está cancelado");
        }
        this.estado = EstadoRegalo.CANCELADO;
    }

    public UUID getId() { return id; }
    public EstadoRegalo getEstado() { return estado; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public UUID getIdProducto() { return idProducto; }
    public UUID getIdUsuario() { return idUsuario; }
    public UUID getIdCombo() { return idCombo; }

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