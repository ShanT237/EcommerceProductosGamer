package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad Combo.
 *  - Identidad: cada combo tiene un id único.
 *  - Pertenece a un Vendedor.
 *  - Regla 5: un combo debe estar formado por dos o más productos distintos.
 */
public class Combo {

    @Getter
    private final UUID id;
    @Getter
    private final UUID vendedorId;
    @Getter
    private final String nombre;
    @Getter
    private final String descripcion;
    @Getter
    private final double descuento;
    private final List<UUID> productos;

    public Combo(UUID id, UUID vendedorId, String nombre, String descripcion, double descuento, List<UUID> productosIniciales) {
        this.id = Objects.requireNonNull(id, "El id del combo es obligatorio");
        this.vendedorId = Objects.requireNonNull(vendedorId, "El id del vendedor es obligatorio");

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

        Set<UUID> sinDuplicados = new HashSet<>(productosIniciales);
        if (sinDuplicados.size() < productosIniciales.size()) {
            throw new ReglaDominioException("Un combo no puede contener productos duplicados");
        }

        this.nombre = nombre;
        this.descripcion = descripcion;
        this.descuento = descuento;
        this.productos = new ArrayList<>(productosIniciales);
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

    /**
     * Calcula el precio total del combo con descuento aplicado sobre la suma de sus productos.
     */
    public double calcularPrecioTotal(List<Producto> productosDelCombo) {
        if (productosDelCombo == null || productosDelCombo.size() != productos.size()) {
            throw new ReglaDominioException("Debe proporcionar la lista completa de productos que conforman el combo");
        }
        double sumaPrecios = productosDelCombo.stream().mapToDouble(Producto::getPrecio).sum();
        return sumaPrecios * (1.0 - (descuento / 100.0));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Combo combo)) return false;
        return id.equals(combo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
