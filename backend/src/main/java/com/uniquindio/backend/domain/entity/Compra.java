package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Compra.
 *  - Identidad: cada compra tiene un id único.
 *  - Ciclo de vida: PENDIENTE -> COMPLETADA -> (reseñada -> puntos asignados) | REEMBOLSADA.
 *  - Guarda el precio unitario vigente al momento de la compra (no una referencia al
 *    precio actual del producto), para que cambios futuros de precio no alteren compras pasadas.
 *  - Regla 2: un comprador no puede calificar un producto sin haberlo comprado.
 *  - Regla 6: los puntos solo se asignan después de realizar una reseña.
 *  - Reembolso: solo dentro de un plazo definido y antes de haber descargado el archivo.
 */
@Getter
public class Compra {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID productoId;
    private final int cantidad;
    private final LocalDate fecha;
    private final Precio precioUnitario;
    private EstadoCompra estado;
    private boolean resenada;
    private boolean puntosAsignados;
    private boolean descargado;

    public Compra(UUID id, UUID usuarioId, UUID productoId, int cantidad, LocalDate fecha, Precio precioUnitario) {
        this.id = Objects.requireNonNull(id, "El id de la compra es obligatorio");
        this.usuarioId = Objects.requireNonNull(usuarioId, "El id del usuario es obligatorio");
        this.productoId = Objects.requireNonNull(productoId, "El id del producto es obligatorio");
        this.fecha = Objects.requireNonNull(fecha, "La fecha de la compra es obligatoria");
        this.precioUnitario = Objects.requireNonNull(precioUnitario, "El precio unitario de la compra es obligatorio");

        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad de la compra debe ser positiva");
        }

        this.cantidad = cantidad;
        this.estado = EstadoCompra.PENDIENTE;
        this.resenada = false;
        this.puntosAsignados = false;
        this.descargado = false;
    }

    /**
     * Subtotal de la compra: precio unitario guardado en el momento de comprar,
     * multiplicado por la cantidad. No depende del precio actual del producto.
     */
    public Precio getSubtotal() {
        return new Precio(precioUnitario.valor() * cantidad);
    }

    /**
     * Confirma el pago de la compra (simulación de pago exitosa).
     */
    public void confirmar() {
        if (estado != EstadoCompra.PENDIENTE) {
            throw new ReglaDominioException("Solo se puede confirmar una compra pendiente");
        }
        this.estado = EstadoCompra.COMPLETADA;
    }

    public void cancelar() {
        if (estado != EstadoCompra.PENDIENTE) {
            throw new ReglaDominioException("Solo se puede cancelar una compra pendiente");
        }
        this.estado = EstadoCompra.CANCELADA;
    }

    public void expirar() {
        if (estado != EstadoCompra.PENDIENTE) {
            throw new ReglaDominioException("Solo se puede expirar una compra pendiente");
        }
        this.estado = EstadoCompra.EXPIRADA;
    }

    /**
     * Marca el archivo/producto como descargado. Una vez descargado,
     * ya no se puede solicitar reembolso.
     */
    public void marcarDescargado() {
        if (estado != EstadoCompra.COMPLETADA) {
            throw new ReglaDominioException("No se puede descargar el producto de una compra no completada");
        }
        this.descargado = true;
    }

    /**
     * Regla: una compra solo puede reembolsarse dentro de un plazo definido
     * y antes de haber descargado el archivo.
     */
    public void solicitarReembolso(LocalDate fechaSolicitud, int plazoDias) {
        Objects.requireNonNull(fechaSolicitud, "La fecha de solicitud es obligatoria");

        if (estado != EstadoCompra.COMPLETADA) {
            throw new ReglaDominioException("Solo se puede reembolsar una compra completada");
        }
        if (descargado) {
            throw new ReglaDominioException("No se puede reembolsar una compra cuyo archivo ya fue descargado");
        }
        if (fechaSolicitud.isAfter(fecha.plusDays(plazoDias))) {
            throw new ReglaDominioException("El plazo para solicitar el reembolso ha expirado");
        }

        this.estado = EstadoCompra.REEMBOLSADA;
    }

    /**
     * Regla 2: un comprador no puede calificar un producto sin haberlo
     * comprado y sin que la compra esté completada.
     */
    public void registrarResena() {
        if (estado != EstadoCompra.COMPLETADA) {
            throw new ReglaDominioException("Solo se puede reseñar una compra completada");
        }
        if (this.resenada) {
            throw new ReglaDominioException("La compra ya fue reseñada");
        }
        this.resenada = true;
    }

    /**
     * Regla 6: los puntos solo se asignan después de realizar una reseña.
     */
    public int asignarPuntos(int puntosPorCompra) {
        if (!this.resenada) {
            throw new ReglaDominioException("No se pueden asignar puntos sin haber reseñado el producto");
        }
        if (this.puntosAsignados) {
            throw new ReglaDominioException("Los puntos de esta compra ya fueron asignados");
        }
        if (puntosPorCompra <= 0) {
            throw new ReglaDominioException("Los puntos por compra deben ser positivos");
        }
        this.puntosAsignados = true;
        return puntosPorCompra;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Compra compra)) return false;
        return id.equals(compra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}