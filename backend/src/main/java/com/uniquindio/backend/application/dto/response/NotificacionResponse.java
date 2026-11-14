package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: GET /api/notificaciones y PUT /api/notificaciones/{id}/leida
public record NotificacionResponse(
        UUID id,
        UUID productoId,
        TipoNotificacion tipo,
        String mensaje,
        LocalDateTime fecha,
        boolean leida
) {
    public static NotificacionResponse desde(Notificacion notificacion) {
        return new NotificacionResponse(
                notificacion.getId(),
                notificacion.getProductoId(),
                notificacion.getTipo(),
                notificacion.getMensaje(),
                notificacion.getFecha(),
                notificacion.isLeida()
        );
    }
}
