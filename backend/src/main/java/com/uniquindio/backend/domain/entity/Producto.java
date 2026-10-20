package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Producto.
 *  - Identidad: dos productos son el mismo solo si comparten el mismo id.
 *  - Pertenece a un Vendedor, que está FUERA del agregado: solo se referencia por id.
 *  - Ciclo de vida: se crea, se vende (reduce stock), puede eliminarse lógicamente.
 *  - Regla 1: el stock no puede ser negativo.
 *  - Regla 3: no se puede eliminar un producto con compras activas (eliminación lógica).
 *  - Regla 9: un producto exclusivo solo puede ser comprado una vez por usuario.
 */
@Getter
public class Producto {

    public static final String CATEGORIA_POR_DEFECTO = "General";

    private final UUID id;
    private final UUID vendedorId;
    private String nombre;
    private String descripcion;
    private double precio;
    /** Precio anterior al descuento activo; {@code null} si no hay descuento. */
    private Double precioOriginal;
    private int stock;
    private final Gama gama;
    private final Exclusividad exclusividad;
    private final String categoria;
    private boolean eliminado;
    private boolean pausado;

    private Producto(UUID id, UUID vendedorId, String nombre, String descripcion,
                     double precio, int stock, Gama gama, Exclusividad exclusividad, String categoria) {
        this.id = id;
        this.vendedorId = vendedorId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.precioOriginal = null;
        this.stock = stock;
        this.gama = gama;
        this.exclusividad = exclusividad;
        this.categoria = categoria;
        this.eliminado = false;
    }

    public static Producto publicar(UUID id, UUID vendedorId, String nombre, String descripcion,
                                    double precio, int stock, Gama gama, Exclusividad exclusividad) {
        return publicar(id, vendedorId, nombre, descripcion, precio, stock, gama, exclusividad,
                CATEGORIA_POR_DEFECTO);
    }

    public static Producto publicar(UUID id, UUID vendedorId, String nombre, String descripcion,
                                    double precio, int stock, Gama gama, Exclusividad exclusividad,
                                    String categoria) {
        Objects.requireNonNull(id, "El id del producto es obligatorio");
        Objects.requireNonNull(vendedorId, "El vendedor del producto es obligatorio");
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
        if (categoria == null || categoria.isBlank()) {
            throw new ReglaDominioException("La categoría del producto no puede estar vacía");
        }
        return new Producto(id, vendedorId, nombre, descripcion, precio, stock, gama, exclusividad, categoria);
    }

    /**
     * Solo para el adaptador de persistencia: "revive" un producto que YA existía.
     * NO valida: esos datos ya pasaron por publicar(...) cuando el producto nació.
     */
    public static Producto reconstruir(UUID id, UUID vendedorId, String nombre, String descripcion,
                                       double precio, int stock, Gama gama, Exclusividad exclusividad,
                                       boolean eliminado) {
        return reconstruir(id, vendedorId, nombre, descripcion, precio, stock, gama, exclusividad,
                eliminado, null);
    }

    public static Producto reconstruir(UUID id, UUID vendedorId, String nombre, String descripcion,
                                       double precio, int stock, Gama gama, Exclusividad exclusividad,
                                       boolean eliminado, Double precioOriginal) {
        return reconstruir(id, vendedorId, nombre, descripcion, precio, stock, gama, exclusividad,
                eliminado, precioOriginal, false);
    }

    public static Producto reconstruir(UUID id, UUID vendedorId, String nombre, String descripcion,
                                       double precio, int stock, Gama gama, Exclusividad exclusividad,
                                       boolean eliminado, Double precioOriginal, boolean pausado) {
        return reconstruir(id, vendedorId, nombre, descripcion, precio, stock, gama, exclusividad,
                eliminado, precioOriginal, pausado, CATEGORIA_POR_DEFECTO);
    }

    public static Producto reconstruir(UUID id, UUID vendedorId, String nombre, String descripcion,
                                       double precio, int stock, Gama gama, Exclusividad exclusividad,
                                       boolean eliminado, Double precioOriginal, boolean pausado,
                                       String categoria) {
        Producto producto = new Producto(id, vendedorId, nombre, descripcion,
                precio, stock, gama, exclusividad, categoria);
        producto.eliminado = eliminado;
        producto.precioOriginal = precioOriginal;
        producto.pausado = pausado;
        return producto;
    }

    public boolean esExclusivo() {
        return exclusividad.esExclusivo();
    }

    public boolean estaVisible() {
        return !eliminado && !pausado;
    }

    public void asegurarNoEliminado() {
        if (eliminado) {
            throw new ReglaDominioException("El producto no se encuentra disponible (eliminado)");
        }
    }

    public void asegurarPerteneceA(UUID vendedorId) {
        if (!Objects.equals(this.vendedorId, vendedorId)) {
            throw new ReglaDominioException("El producto no pertenece al vendedor");
        }
    }

    /**
     * Un producto solo se puede comprar si no está eliminado ni pausado.
     */
    public void asegurarComprable() {
        asegurarNoEliminado();
        if (pausado) {
            throw new ReglaDominioException("El producto se encuentra pausado");
        }
    }

    public void pausar() {
        asegurarNoEliminado();
        if (pausado) {
            throw new ReglaDominioException("El producto ya está pausado");
        }
        this.pausado = true;
    }

    public void activar() {
        asegurarNoEliminado();
        if (!pausado) {
            throw new ReglaDominioException("El producto ya está activo");
        }
        this.pausado = false;
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

    /**
     * Verifica que hay stock para la cantidad pedida SIN modificarlo; permite validar
     * varios productos antes de descontar cualquiera (compra de combos).
     */
    public void asegurarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad debe ser positiva");
        }
        if (this.stock < cantidad) {
            throw new ReglaDominioException("No hay suficiente stock disponible");
        }
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
        if (eliminado) {
            throw new ReglaDominioException("El producto ya está eliminado");
        }
        if (tieneComprasActivas) {
            throw new ReglaDominioException("No se puede eliminar un producto que tiene compras activas");
        }
        this.eliminado = true;
    }

    /**
     * Regla 9: un producto exclusivo solo puede ser comprado una vez por el mismo usuario.
     * Quien invoca este método (el caso de uso) es responsable de averiguar, vía el
     * repositorio, si el usuario ya tiene una compra de este producto; el Producto
     * solo decide qué hacer con ese dato.
     */
    public void validarCompraExclusiva(boolean usuarioYaTieneEsteProducto, int cantidad) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad debe ser positiva");
        }
        if (!esExclusivo()) {
            return;
        }
        if (cantidad > 1) {
            throw new ReglaDominioException(
                    "Un producto exclusivo solo puede comprarse en cantidad de una unidad");
        }
        if (usuarioYaTieneEsteProducto) {
            throw new ReglaDominioException(
                    "El usuario ya compró este producto exclusivo anteriormente");
        }
    }

    /**
     * Cambia el precio base del producto. El precio debe seguir siendo positivo
     * y un producto eliminado no se modifica.
     */
    public void cambiarPrecio(double nuevoPrecio) {
        asegurarNoEliminado();
        if (nuevoPrecio <= 0) {
            throw new ReglaDominioException("El precio del producto debe ser positivo");
        }
        this.precio = nuevoPrecio;
    }

    /**
     * Actualiza nombre y descripción; ambos siguen siendo obligatorios.
     */
    public void editarInformacion(String nuevoNombre, String nuevaDescripcion) {
        asegurarNoEliminado();
        if (nuevoNombre == null || nuevoNombre.isBlank()) {
            throw new ReglaDominioException("El nombre del producto no puede estar vacío");
        }
        if (nuevaDescripcion == null || nuevaDescripcion.isBlank()) {
            throw new ReglaDominioException("La descripción del producto no puede estar vacía");
        }
        this.nombre = nuevoNombre;
        this.descripcion = nuevaDescripcion;
    }

    public void asegurarPerteneceA(UUID vendedorId) {
        if (!this.vendedorId.equals(vendedorId)) {
            throw new ReglaDominioException("El producto no pertenece a este vendedor");
        }
    }

    public void aplicarDescuento(double porcentaje) {
        asegurarNoEliminado();
        if (precioOriginal != null) {
            throw new ReglaDominioException("El producto ya tiene un descuento activo");
        }
        if (porcentaje <= 0) {
            throw new ReglaDominioException("El descuento debe ser mayor a cero");
        }
        this.precioOriginal = this.precio;
        this.precio = new Precio(this.precio).aplicarDescuento(porcentaje).valor();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Producto producto)) return false;
        return id.equals(producto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}