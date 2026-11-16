package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Preventa.
 *  - Identidad: cada preventa tiene un id único.
 *  - Ciclo de vida: se crea con una fecha de lanzamiento futura, se puede consultar su disponibilidad
 *    y, mientras no haya sido lanzada, se puede reprogramar (fecha y descripción) o cancelar.
 *  - Regla 4: un usuario no puede tener acceso a un producto de preventa antes de la fecha estipulada.
 */
@Getter
public class Preventa {

    private final UUID id;
    private final UUID productoId;
    private LocalDate fechaLanzamiento;
    private String descripcion;

    // Preventa
    private Preventa(UUID id, UUID productoId, LocalDate fechaLanzamiento, String descripcion) {
        this.id = id;
        this.productoId = productoId;
        this.fechaLanzamiento = fechaLanzamiento;
        this.descripcion = descripcion;
    }


    public static Preventa reconstruir(UUID id, UUID productoId, LocalDate fechaLanzamiento, String descripcion) {
        return new Preventa(id, productoId, fechaLanzamiento, descripcion);
    }

    public static Preventa programar(UUID id, UUID productoId, LocalDate fechaLanzamiento, String descripcion) {
        Objects.requireNonNull(id, "El id de la preventa es obligatorio");
        Objects.requireNonNull(productoId, "El id del producto es obligatorio");
        Objects.requireNonNull(fechaLanzamiento, "La fecha de lanzamiento es obligatoria");
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción de la preventa no puede estar vacía");
        }
        return new Preventa(id, productoId, fechaLanzamiento, descripcion);
    }

    /**
     * Factory de programación: exige que la fecha de lanzamiento sea estrictamente posterior a {@code hoy}.
     */
    public static Preventa programar(UUID id, UUID productoId, LocalDate fechaLanzamiento,
                                     String descripcion, LocalDate hoy) {
        Objects.requireNonNull(hoy, "La fecha actual es obligatoria");
        if (fechaLanzamiento == null) {
            throw new ReglaDominioException("La fecha de lanzamiento es obligatoria");
        }
        if (!fechaLanzamiento.isAfter(hoy)) {
            throw new ReglaDominioException("La fecha de lanzamiento debe ser futura");
        }
        return programar(id, productoId, fechaLanzamiento, descripcion);
    }

    /**
     * Modifica la fecha de lanzamiento y la descripción de una preventa que todavía no se ha lanzado.
     * La nueva fecha debe ser estrictamente posterior a {@code hoy} (igual que al programarla) y la
     * descripción sigue siendo obligatoria. Si falla alguna validación, la preventa queda intacta.
     */
    public void reprogramar(LocalDate nuevaFecha, String nuevaDescripcion, LocalDate hoy) {
        Objects.requireNonNull(hoy, "La fecha actual es obligatoria");
        asegurarNoLanzada(hoy, "modificar");
        if (nuevaFecha == null) {
            throw new ReglaDominioException("La fecha de lanzamiento es obligatoria");
        }
        if (!nuevaFecha.isAfter(hoy)) {
            throw new ReglaDominioException("La fecha de lanzamiento debe ser futura");
        }
        if (nuevaDescripcion == null || nuevaDescripcion.isBlank()) {
            throw new ReglaDominioException("La descripción de la preventa no puede estar vacía");
        }
        this.fechaLanzamiento = nuevaFecha;
        this.descripcion = nuevaDescripcion;
    }

    /**
     * Una preventa solo se puede modificar o cancelar mientras no se haya lanzado: a partir de la
     * fecha de lanzamiento el producto ya es accesible para los compradores.
     */
    public void asegurarNoLanzada(LocalDate hoy, String accion) {
        Objects.requireNonNull(hoy, "La fecha actual es obligatoria");
        if (estaDisponible(hoy)) {
            throw new ReglaDominioException(
                    "La preventa ya fue lanzada y no se puede " + accion);
        }
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
        if (!(o instanceof Preventa preventa)) return false;
        return id.equals(preventa.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
