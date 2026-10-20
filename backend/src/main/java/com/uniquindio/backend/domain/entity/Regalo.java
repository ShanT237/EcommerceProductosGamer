package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import lombok.Getter;

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
@Getter
public class Regalo {

    private final UUID id;
    private EstadoRegalo estado;
    private final LocalDateTime fechaCreacion;
    private LocalDateTime fechaEntrega;
    private final UUID idProducto;
    private UUID idUsuario;
    private final UUID idCombo;
    private boolean notificado;

    private Regalo(UUID id, UUID idProducto, UUID idCombo) {
        this.id = Objects.requireNonNull(id, "El id del regalo es obligatorio");
        this.idProducto = idProducto;
        this.idCombo = idCombo;
        this.estado = EstadoRegalo.DISPONIBLE;
        this.fechaCreacion = LocalDateTime.now();
    }

    public static Regalo crear(UUID id, UUID idProducto, UUID idCombo) {
        if ((idProducto == null) == (idCombo == null)) {
            throw new ReglaDominioException(
                    "Un regalo debe estar asociado a un producto o a un combo, pero no a ambos");
        }
        return new Regalo(id, idProducto, idCombo);
    }


    public static Regalo reconstruir(UUID id, EstadoRegalo estado, LocalDateTime fechaCreacion,
                                     LocalDateTime fechaEntrega, UUID idProducto, UUID idUsuario,
                                     UUID idCombo, boolean notificado) {
        Regalo regalo = new Regalo(id, idProducto, idCombo);
        regalo.estado = estado;
        regalo.fechaEntrega = fechaEntrega;
        regalo.idUsuario = idUsuario;
        regalo.notificado = notificado;
        return regalo;
    }

    private void verificarTransicion(EstadoRegalo siguiente) {
        if (estado.esFinal()) {
            throw new ReglaDominioException(
                    "Un regalo " + estado.name().toLowerCase() + " no puede cambiar de estado");
        }
        if (!estado.puedeTransicionarA(siguiente)) {
            throw new ReglaDominioException(
                    "No se puede pasar de " + estado + " a " + siguiente);
        }
    }

    public void elegir(UUID idUsuario, boolean usuarioYaPoseeElProducto) {
        Objects.requireNonNull(idUsuario, "El id del usuario es obligatorio para elegir el regalo");

        verificarTransicion(EstadoRegalo.ELEGIDO);
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
        if (this.notificado) {
            throw new ReglaDominioException("El regalo ya fue notificado al usuario");
        }
        this.notificado = true;
    }

    public void enviar() {
        verificarTransicion(EstadoRegalo.ENVIADO);
        this.estado = EstadoRegalo.ENVIADO;
    }

    public void confirmarEntrega(UUID idUsuarioQueConfirma) {
        verificarTransicion(EstadoRegalo.ENTREGADO);
        if (!this.idUsuario.equals(idUsuarioQueConfirma)) {
            throw new ReglaDominioException("Solo el usuario destinatario puede confirmar la recepción");
        }
        this.estado = EstadoRegalo.ENTREGADO;
        this.fechaEntrega = LocalDateTime.now();
    }

    public void cancelarRegalo() {
        verificarTransicion(EstadoRegalo.CANCELADO);
        this.estado = EstadoRegalo.CANCELADO;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Regalo regalo)) return false;
        return id.equals(regalo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}