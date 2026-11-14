package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.NotificacionResponse;
import com.uniquindio.backend.domain.entity.Notificacion;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NotificacionMapper {

    public NotificacionResponse toResponse(Notificacion notificacion) {
        return NotificacionResponse.desde(notificacion);
    }

    public List<NotificacionResponse> toResponses(List<Notificacion> notificaciones) {
        return notificaciones.stream().map(this::toResponse).toList();
    }
}
