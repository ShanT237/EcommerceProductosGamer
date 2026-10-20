package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.NotificacionEntity;
import org.springframework.stereotype.Component;

@Component
public class NotificacionPersistenceMapper {

    public NotificacionEntity toEntity(Notificacion notificacion) {
        NotificacionEntity entity = new NotificacionEntity();
        entity.setId(notificacion.getId());
        entity.setUsuarioId(notificacion.getUsuarioId());
        entity.setProductoId(notificacion.getProductoId());
        entity.setTipo(notificacion.getTipo());
        entity.setMensaje(notificacion.getMensaje());
        entity.setFecha(notificacion.getFecha());
        entity.setLeida(notificacion.isLeida());
        return entity;
    }

    public Notificacion toDomain(NotificacionEntity entity) {
        return Notificacion.reconstruir(
                entity.getId(),
                entity.getUsuarioId(),
                entity.getProductoId(),
                entity.getTipo(),
                entity.getMensaje(),
                entity.getFecha(),
                entity.isLeida()
        );
    }
}