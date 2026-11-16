package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Notificacion;

import com.uniquindio.backend.domain.valueobject.TipoNotificacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {
    void guardar(Notificacion notificacion);
    Optional<Notificacion> obtenerPorId(UUID id);
    List<Notificacion> obtenerPorUsuarioId(UUID usuarioId);

    /** Evita avisos duplicados (p. ej. la preventa lanzada se avisa una sola vez por usuario y producto). */
    boolean existe(UUID usuarioId, UUID productoId, TipoNotificacion tipo);
}
