package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Regalo;

import com.uniquindio.backend.domain.valueobject.EstadoRegalo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Regalo.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar regalos.
 */
public interface RegaloRepository {

    Optional<Regalo> obtenerPorId(UUID id);

    void guardar(Regalo regalo);

    /** Regalos en un estado dado (p. ej. DISPONIBLE para el catálogo de regalos). */
    List<Regalo> obtenerPorEstado(EstadoRegalo estado);

    /** Regalos asignados a un usuario, en cualquier estado. */
    List<Regalo> obtenerPorUsuarioId(UUID usuarioId);

    long contarAsignadosA(UUID usuarioId);

    /** true si hay algún regalo del combo que no esté en estado final (ENTREGADO o CANCELADO). */
    boolean existePendienteDelCombo(UUID comboId);
}
