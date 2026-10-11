package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Usuario.
 *  - Identidad: dos usuarios son el mismo solo si comparten el mismo id.
 *  - Ciclo de vida: se crea, acumula/redime puntos con el tiempo, se puede desactivar.
 *  - No se reemplaza por otro usuario aunque cambien sus datos.
 */
@Getter
public class Usuario {

    private final UUID id;
    private final String nombre;
    private String correo;
    private int puntos;
    private boolean activo;

    public Usuario(UUID id, String nombre, String correo) {
        this.id = Objects.requireNonNull(id, "El id del usuario es obligatorio");

        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del usuario no puede estar vacío");
        }
        if (correo == null || !correo.contains("@")) {
            throw new ReglaDominioException("El correo del usuario no es válido");
        }

        this.nombre = nombre;
        this.correo = correo;
        this.puntos = 0;
        this.activo = true;
    }

    public void acumularPuntos(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad de puntos a acumular debe ser positiva");
        }
        this.puntos += cantidad;
    }

    public void redimirPuntos(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad de puntos a redimir debe ser positiva");
        }
        if (this.puntos < cantidad) {
            throw new ReglaDominioException("Saldo de puntos insuficiente");
        }
        this.puntos -= cantidad;
    }

    public void asegurarActivo() {
        if (!activo) {
            throw new ReglaDominioException("El usuario se encuentra inactivo");
        }
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public void actualizarCorreo(String nuevoCorreo) {
        if (nuevoCorreo == null || !nuevoCorreo.contains("@")) {
            throw new ReglaDominioException("El correo del usuario no es válido");
        }
        this.correo = nuevoCorreo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario usuario)) return false;
        return id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}