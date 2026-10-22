package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.PromocionRepository;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PromocionRepositoryEnMemoria implements PromocionRepository {

    private final Map<UUID, Promocion> promociones = new HashMap<>();

    @Override
    public void guardar(Promocion promocion) {
        promociones.put(promocion.getId(), promocion);
    }

    @Override
    public Optional<Promocion> obtenerVigentePorProductoId(UUID productoId, LocalDate fecha) {
        return promociones.values().stream()
                .filter(p -> p.getProductoId().equals(productoId) && p.estaVigente(fecha))
                .findFirst();
    }

    @Override
    public List<Promocion> obtenerPorProductoId(UUID productoId) {
        return promociones.values().stream()
                .filter(p -> p.getProductoId().equals(productoId))
                .toList();
    }
}
