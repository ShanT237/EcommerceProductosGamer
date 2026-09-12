package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Usuario.
 *  - Identidad: dos usuarios son el mismo solo si comparten el mismo id.
 *  - Ciclo de vida: se crea, acumula puntos con el tiempo, se puede eliminar.
 *  - No se reemplaza por otro usuario aunque cambien sus datos.
 */
public class Usuario {

    private final UUID id;
    private String nombre;
    private String correo;
    private int puntos;

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
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public int getPuntos() {
        return puntos;
    }

    public void acumularPuntos(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad de puntos a acumular debe ser positiva");
        }
        this.puntos += cantidad;
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
        if (!(o instanceof Usuario)) return false;
        Usuario usuario = (Usuario) o;
        return id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}