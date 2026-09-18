package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Preventa.
 *  - Identidad: cada preventa tiene un id único.
 *  - Ciclo de vida: se crea con una fecha de lanzamiento futura, se puede consultar su disponibilidad.
 *  - Regla 4: un usuario no puede tener acceso a un producto de preventa antes de la fecha estipulada.
 */
public class Preventa {

    private final UUID id;
    private final UUID productoId;
    private final LocalDate fechaLanzamiento;
    private String descripcion;

    public Preventa(UUID id, UUID productoId, LocalDate fechaLanzamiento, String descripcion) {
        this.id = Objects.requireNonNull(id, "El id de la preventa es obligatorio");
        this.productoId = Objects.requireNonNull(productoId, "El id del producto es obligatorio");
        this.fechaLanzamiento = Objects.requireNonNull(fechaLanzamiento, "La fecha de lanzamiento es obligatoria");

        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción de la preventa no puede estar vacía");
        }

        this.descripcion = descripcion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProductoId() {
        return productoId;
    }

    public LocalDate getFechaLanzamiento() {
        return fechaLanzamiento;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Indica si el producto de preventa ya está disponible para acceso.
     */
    public boolean estaDisponible(LocalDate fechaActual) {
        Objects.requireNonNull(fechaActual, "La fecha actual es obligatoria");
        return !fechaActual.isBefore(fechaLanzamiento);
    }

    /**
     * Regla del negocio: no se puede acceder a un producto de preventa antes de la fecha estipulada.
     */
    public void validarAcceso(LocalDate fechaActual) {
        if (!estaDisponible(fechaActual)) {
            throw new ReglaDominioException(
                    "No se puede acceder al producto de preventa antes de la fecha de lanzamiento"
            );
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Preventa)) return false;
        Preventa preventa = (Preventa) o;
        return id.equals(preventa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
