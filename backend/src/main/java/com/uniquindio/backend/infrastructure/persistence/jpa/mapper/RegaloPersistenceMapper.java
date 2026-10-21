package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.RegaloEntity;
import org.springframework.stereotype.Component;

@Component
public class RegaloPersistenceMapper {

    public RegaloEntity toEntity(Regalo regalo) {
        RegaloEntity entity = new RegaloEntity();
        entity.setId(regalo.getId());
        entity.setEstado(regalo.getEstado());
        entity.setFechaCreacion(regalo.getFechaCreacion());
        entity.setFechaEntrega(regalo.getFechaEntrega());
        entity.setIdProducto(regalo.getIdProducto());
        entity.setIdUsuario(regalo.getIdUsuario());
        entity.setIdCombo(regalo.getIdCombo());
        entity.setNotificado(regalo.isNotificado());
        return entity;
    }

    public Regalo toDomain(RegaloEntity entity) {
        return Regalo.reconstruir(
                entity.getId(),
                entity.getEstado(),
                entity.getFechaCreacion(),
                entity.getFechaEntrega(),
                entity.getIdProducto(),
                entity.getIdUsuario(),
                entity.getIdCombo(),
                entity.isNotificado()
        );
    }
}