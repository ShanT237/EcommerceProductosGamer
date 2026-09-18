package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Producto.
 *  - Identidad: dos productos son el mismo solo si comparten el mismo id.
 *  - Ciclo de vida: se crea, se vende (reduce stock), puede eliminarse lógicamente.
 *  - Regla 1: el stock no puede ser negativo.
 *  - Regla 3: no se puede eliminar un producto con compras activas (eliminación lógica).
 *  - Regla 9: un producto exclusivo solo puede ser comprado una vez por usuario.
 */
public class Producto {

    private final UUID id;
    private String nombre;
    private String descripcion;
    private double precio;
    private int stock;
    private final Gama gama;
    private final Exclusividad exclusividad;
    private boolean eliminado;

    public Producto(UUID id, String nombre, String descripcion, double precio,
                    int stock, Gama gama, Exclusividad exclusividad) {
        this.id = Objects.requireNonNull(id, "El id del producto es obligatorio");
        Objects.requireNonNull(gama, "La gama del producto es obligatoria");
        Objects.requireNonNull(exclusividad, "La exclusividad del producto es obligatoria");

        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del producto no puede estar vacío");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción del producto no puede estar vacía");
        }
        if (precio <= 0) {
            throw new ReglaDominioException("El precio del producto debe ser positivo");
        }
        if (stock < 0) {
            throw new ReglaDominioException("El stock del producto no puede ser negativo");
        }

        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.stock = stock;
        this.gama = gama;
        this.exclusividad = exclusividad;
        this.eliminado = false;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public double getPrecio() {
        return precio;
    }

    public int getStock() {
        return stock;
    }

    public Gama getGama() {
        return gama;
    }

    public Exclusividad getExclusividad() {
        return exclusividad;
    }

    public boolean isEliminado() {
        return eliminado;
    }

    public boolean esExclusivo() {
        return exclusividad.esExclusivo();
    }

    /**
     * Regla 1: el stock no puede ser negativo.
     */
    public void reducirStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad a reducir debe ser positiva");
        }
        if (this.stock - cantidad < 0) {
            throw new ReglaDominioException("No hay suficiente stock disponible");
        }
        this.stock -= cantidad;
    }

    public void aumentarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad a aumentar debe ser positiva");
        }
        this.stock += cantidad;
    }

    /**
     * Regla 3: no se puede eliminar un producto que tenga compras activas.
     * Cuando no tiene compras activas se marca como eliminado lógicamente.
     */
    public void eliminar(boolean tieneComprasActivas) {
        if (tieneComprasActivas) {
            throw new ReglaDominioException(
                    "No se puede eliminar un producto que tiene compras activas"
            );
        }
        this.eliminado = true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto)) return false;
        Producto producto = (Producto) o;
        return id.equals(producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
