package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Notificacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {
    void guardar(Notificacion notificacion);
    Optional<Notificacion> obtenerPorId(UUID id);
    List<Notificacion> obtenerPorUsuarioId(UUID usuarioId);
}
