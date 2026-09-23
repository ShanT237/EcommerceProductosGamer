package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Precio;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Compra.
 *  - Identidad: cada compra tiene un id único.
 *  - Ciclo de vida: se crea al realizarse, puede recibir una reseña, y luego asignar puntos.
 *  - Guarda el precio unitario vigente al momento de la compra (no una referencia al
 *    precio actual del producto), para que cambios futuros de precio no alteren compras pasadas.
 *  - Regla 2: un comprador no puede calificar un producto sin haberlo comprado.
 *  - Regla 6: los puntos solo se asignan después de realizar una reseña.
 */
public class Compra {

    private final UUID id;
    private final UUID usuarioId;
    private final UUID productoId;
    private final int cantidad;
    private final LocalDate fecha;
    private final Precio precioUnitario;
    private boolean resenada;
    private boolean puntosAsignados;

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
        this.resenada = false;
        this.puntosAsignados = false;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getProductoId() { return productoId; }
    public int getCantidad() { return cantidad; }
    public LocalDate getFecha() { return fecha; }
    public Precio getPrecioUnitario() { return precioUnitario; }
    public boolean isResenada() { return resenada; }
    public boolean isPuntosAsignados() { return puntosAsignados; }

    /**
     * Subtotal de la compra: precio unitario guardado en el momento de comprar,
     * multiplicado por la cantidad. No depende del precio actual del producto.
     */
    public Precio getSubtotal() {
        return new Precio(precioUnitario.valor() * cantidad);
    }

    public void registrarResena() {
        if (this.resenada) {
            throw new ReglaDominioException("La compra ya fue reseñada");
        }
        this.resenada = true;
    }

    public int asignarPuntos(int puntosPorCompra) {
        if (!this.resenada) {
            throw new ReglaDominioException(
                    "No se pueden asignar puntos sin haber reseñado el producto"
            );
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
        if (!(o instanceof Compra)) return false;
        Compra compra = (Compra) o;
        return id.equals(compra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}