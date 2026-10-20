package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Cupon (CU-17).
 *  - Identidad: id único; el código se normaliza a mayúsculas.
 *  - Descuento porcentual (mayor a 0 y menor a 100), vigente hasta una fecha inclusive,
 *    con un máximo de usos.
 */
@Getter
public class Cupon {

    private final UUID id;
    private final String codigo;
    private final double porcentaje;
    private final LocalDate vence;
    private final int usosMaximos;
    private int usos;

    private Cupon(UUID id, String codigo, double porcentaje, LocalDate vence, int usosMaximos) {
        this.id = id;
        this.codigo = codigo;
        this.porcentaje = porcentaje;
        this.vence = vence;
        this.usosMaximos = usosMaximos;
        this.usos = 0;
    }

    public static Cupon crear(UUID id, String codigo, double porcentaje, LocalDate vence, int usosMaximos) {
        Objects.requireNonNull(id, "El id del cupón es obligatorio");
        Objects.requireNonNull(vence, "La fecha de vencimiento es obligatoria");
        if (codigo == null || codigo.isBlank()) {
            throw new ReglaDominioException("El código del cupón no puede estar vacío");
        }
        if (porcentaje <= 0 || porcentaje >= 100) {
            throw new ReglaDominioException("El descuento debe ser mayor a 0 y menor que 100");
        }
        if (usosMaximos <= 0) {
            throw new ReglaDominioException("Los usos máximos deben ser positivos");
        }
        return new Cupon(id, codigo.trim().toUpperCase(), porcentaje, vence, usosMaximos);
    }

    public void validarUso(LocalDate hoy) {
        if (hoy.isAfter(vence)) {
            throw new ReglaDominioException("El cupón está vencido");
        }
        if (usos >= usosMaximos) {
            throw new ReglaDominioException("El cupón ya no tiene usos disponibles");
        }
    }

    public void registrarUso() {
        if (usos >= usosMaximos) {
            throw new ReglaDominioException("El cupón ya no tiene usos disponibles");
        }
        this.usos++;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cupon cupon)) return false;
        return id.equals(cupon.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
