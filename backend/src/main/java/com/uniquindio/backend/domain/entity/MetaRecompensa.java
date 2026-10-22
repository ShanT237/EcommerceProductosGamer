package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Entidad MetaRecompensa (CU-33): umbral de compras completadas que desbloquea un regalo
 * (el producto regalo se referencia por id). Se desactiva, no se borra.
 */
@Getter
public class MetaRecompensa {

    private final UUID id;
    private int comprasRequeridas;
    private final UUID productoRegaloId;
    private boolean activa;

    private MetaRecompensa(UUID id, int comprasRequeridas, UUID productoRegaloId) {
        this.id = id;
        this.comprasRequeridas = comprasRequeridas;
        this.productoRegaloId = productoRegaloId;
        this.activa = true;
    }

    public static MetaRecompensa crear(UUID id, int comprasRequeridas, UUID productoRegaloId) {
        Objects.requireNonNull(id, "El id de la meta es obligatorio");
        Objects.requireNonNull(productoRegaloId, "El producto regalo es obligatorio");
        validarUmbral(comprasRequeridas);
        return new MetaRecompensa(id, comprasRequeridas, productoRegaloId);
    }

    /**
     * Solo para el adaptador de persistencia: "revive" una meta que YA existía.
     * NO valida: eso ya ocurrió en crear(...).
     */
    public static MetaRecompensa reconstruir(UUID id, int comprasRequeridas,
                                             UUID productoRegaloId, boolean activa) {
        MetaRecompensa meta = new MetaRecompensa(id, comprasRequeridas, productoRegaloId);
        meta.activa = activa;
        return meta;
    }

    public void actualizarUmbral(int nuevoUmbral) {
        validarUmbral(nuevoUmbral);
        this.comprasRequeridas = nuevoUmbral;
    }

    public void desactivar() {
        if (!activa) {
            throw new ReglaDominioException("La meta ya está desactivada");
        }
        this.activa = false;
    }

    public boolean seAlcanzaCon(long comprasCompletadas) {
        return comprasCompletadas >= comprasRequeridas;
    }

    private static void validarUmbral(int umbral) {
        if (umbral <= 0) {
            throw new ReglaDominioException("El umbral de compras debe ser positivo");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetaRecompensa meta)) return false;
        return id.equals(meta.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
