package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Promocion;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PromocionRepository {
    void guardar(Promocion promocion);
    Optional<Promocion> obtenerVigentePorProductoId(UUID productoId, LocalDate fecha);
    List<Promocion> obtenerPorProductoId(UUID productoId);

    /**
     * Promoción vigente de cada producto pedido (los que no tienen no aparecen en el mapa).
     * La implementación por defecto consulta de a uno; el adaptador JPA la sobreescribe con una sola consulta.
     */
    default Map<UUID, Promocion> obtenerVigentesPorProductoIds(Collection<UUID> productoIds, LocalDate fecha) {
        Map<UUID, Promocion> vigentes = new HashMap<>();
        for (UUID id : productoIds) {
            obtenerVigentePorProductoId(id, fecha).ifPresent(promocion -> vigentes.put(id, promocion));
        }
        return vigentes;
    }

    Optional<Promocion> obtenerPorId(UUID id);

    void eliminar(UUID id);

    /** Todas las promociones (pasadas, vigentes y futuras) de los productos pedidos. */
    default List<Promocion> obtenerPorProductoIds(Collection<UUID> productoIds) {
        List<Promocion> resultado = new java.util.ArrayList<>();
        for (UUID id : productoIds) {
            resultado.addAll(obtenerPorProductoId(id));
        }
        return resultado;
    }
}
