package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Promocion;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromocionRepository {
    void guardar(Promocion promocion);
    Optional<Promocion> obtenerVigentePorProductoId(UUID productoId, LocalDate fecha);
    List<Promocion> obtenerPorProductoId(UUID productoId);
}
