package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Preventa;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PreventaRepository {

    /** Usado por CU-37 para rechazar una segunda preventa del mismo producto. */
    Optional<Preventa> obtenerPorProductoId(UUID productoId);

    /** Preventas con fecha de lanzamiento estrictamente posterior a hoy. */
    List<Preventa> obtenerFuturas(LocalDate hoy);

    /** Preventas cuya fecha de lanzamiento es exactamente {@code fecha}. */
    List<Preventa> obtenerLanzadasEn(LocalDate fecha);

    void guardar(Preventa preventa);
}
