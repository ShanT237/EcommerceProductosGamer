package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;

import java.time.LocalDateTime;
import java.util.UUID;

// Usado en: PUT /api/regalos/{id}/asignar (CU-10) y los endpoints de envío/entrega (CU-24)
public record RegaloResponse(
        UUID id,
        EstadoRegalo estado,
        UUID idProducto,
        UUID idCombo,
        UUID idUsuario,
        boolean notificado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEntrega
) {
    public static RegaloResponse desde(Regalo regalo) {
        return new RegaloResponse(
                regalo.getId(),
                regalo.getEstado(),
                regalo.getIdProducto(),
                regalo.getIdCombo(),
                regalo.getIdUsuario(),
                regalo.isNotificado(),
                regalo.getFechaCreacion(),
                regalo.getFechaEntrega()
        );
    }
}