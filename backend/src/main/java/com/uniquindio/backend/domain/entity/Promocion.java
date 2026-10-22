package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Promocion (CU-22).
 *  - Identidad: cada promoción tiene un id único; pertenece a un Producto (por id).
 *  - Descuento porcentual (mayor a 0 y menor a 100) con vigencia entre dos fechas, inclusive.
 *  - No modifica el precio base del producto: solo se aplica mientras está vigente.
 */
@Getter
public class Promocion {

    private final UUID id;
    private final UUID productoId;
    private final double porcentaje;
    private final LocalDate desde;
    private final LocalDate hasta;

    private Promocion(UUID id, UUID productoId, double porcentaje, LocalDate desde, LocalDate hasta) {
        this.id = id;
        this.productoId = productoId;
        this.porcentaje = porcentaje;
        this.desde = desde;
        this.hasta = hasta;
    }

    public static Promocion programar(UUID id, UUID productoId, double porcentaje,
                                      LocalDate desde, LocalDate hasta) {
        Objects.requireNonNull(id, "El id de la promoción es obligatorio");
        Objects.requireNonNull(productoId, "El producto de la promoción es obligatorio");
        Objects.requireNonNull(desde, "La fecha de inicio es obligatoria");
        Objects.requireNonNull(hasta, "La fecha de fin es obligatoria");
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new ReglaDominioException("El descuento debe ser mayor a 0 y menor que 100");
        }
        if (hasta.isBefore(desde)) {
            throw new ReglaDominioException("La fecha de fin no puede ser anterior a la de inicio");
        }
        return new Promocion(id, productoId, porcentaje, desde, hasta);
    }

    /**
     * Solo para el adaptador de persistencia: "revive" una promoción que YA existía.
     * NO valida: eso ya ocurrió en programar(...).
     */
    public static Promocion reconstruir(UUID id, UUID productoId, double porcentaje,
                                        LocalDate desde, LocalDate hasta) {
        return new Promocion(id, productoId, porcentaje, desde, hasta);
    }

    public boolean estaVigente(LocalDate fecha) {
        return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }

    public boolean seSolapaCon(LocalDate otraDesde, LocalDate otraHasta) {
        return !otraDesde.isAfter(hasta) && !otraHasta.isBefore(desde);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Promocion promocion)) return false;
        return id.equals(promocion.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
