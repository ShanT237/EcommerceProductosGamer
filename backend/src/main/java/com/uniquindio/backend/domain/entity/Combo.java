package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Combo.
 *  - Identidad: cada combo tiene un id único.
 *  - Regla 5: un combo debe estar formado por dos o más productos.
 */
public class Combo {

    private final UUID id;
    private String nombre;
    private String descripcion;
    private double descuento;
    private final List<UUID> productos;

    public Combo(UUID id, String nombre, String descripcion, double descuento, List<UUID> productosIniciales) {
        this.id = Objects.requireNonNull(id, "El id del combo es obligatorio");

        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDominioException("El nombre del combo no puede estar vacío");
        }
        if (descripcion == null || descripcion.isBlank()) {
            throw new ReglaDominioException("La descripción del combo no puede estar vacía");
        }
        if (descuento < 0 || descuento > 100) {
            throw new ReglaDominioException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        if (productosIniciales == null || productosIniciales.size() < 2) {
            throw new ReglaDominioException("Un combo debe estar formado por dos o más productos");
        }

        this.nombre = nombre;
        this.descripcion = descripcion;
        this.descuento = descuento;
        this.productos = new ArrayList<>(productosIniciales);
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

    public double getDescuento() {
        return descuento;
    }

    public List<UUID> getProductos() {
        return List.copyOf(productos);
    }

    public void agregarProducto(UUID productoId) {
        Objects.requireNonNull(productoId, "El id del producto es obligatorio");
        if (productos.contains(productoId)) {
            throw new ReglaDominioException("El producto ya se encuentra en el combo");
        }
        productos.add(productoId);
    }

    public void quitarProducto(UUID productoId) {
        if (!productos.contains(productoId)) {
            throw new ReglaDominioException("El producto no se encuentra en el combo");
        }
        if (productos.size() - 1 < 2) {
            throw new ReglaDominioException("Un combo debe tener al menos dos productos");
        }
        productos.remove(productoId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Combo)) return false;
        Combo combo = (Combo) o;
        return id.equals(combo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
