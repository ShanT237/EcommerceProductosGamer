package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad Wishlist.
 *  - Identidad: cada lista tiene entidad propia ligada a un usuario.
 *  - No se reemplaza por otra lista: se modifica (se agregan/quitan productos).
 *  - Ciclo de vida: nace vacía, se modifica en el tiempo, no se recrea con cada cambio.
 */
public class Wishlist {

    private final UUID id;
    private final UUID usuarioId;
    private final List<UUID> productos;

    public Wishlist(UUID id, UUID usuarioId) {
        this.id = Objects.requireNonNull(id, "El id de la wishlist es obligatorio");
        this.usuarioId = Objects.requireNonNull(usuarioId, "La wishlist debe pertenecer a un usuario");
        this.productos = new ArrayList<>();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUsuarioId() {
        return usuarioId;
    }

    public List<UUID> getProductos() {
        return List.copyOf(productos);
    }

    /**
     * Regla del negocio: no se permite el mismo producto dos veces en la wishlist.
     */
    public void agregarProducto(UUID productoId) {
        Objects.requireNonNull(productoId, "El id del producto es obligatorio");
        if (productos.contains(productoId)) {
            throw new ReglaDominioException("El producto ya se encuentra en la wishlist");
        }
        productos.add(productoId);
    }

    public void quitarProducto(UUID productoId) {
        if (!productos.remove(productoId)) {
            throw new ReglaDominioException("El producto no está en la wishlist");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wishlist)) return false;
        Wishlist wishlist = (Wishlist) o;
        return id.equals(wishlist.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}