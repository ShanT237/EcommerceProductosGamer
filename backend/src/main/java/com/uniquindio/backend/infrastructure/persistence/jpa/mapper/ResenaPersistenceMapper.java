package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ResenaEntity;
import org.springframework.stereotype.Component;

@Component
public class ResenaPersistenceMapper {

    public ResenaEntity toEntity(Resena resena) {
        ResenaEntity entity = new ResenaEntity();
        entity.setId(resena.getId());
        entity.setIdCompra(resena.getIdCompra());
        entity.setIdProducto(resena.getIdProducto());
        entity.setIdUsuario(resena.getIdUsuario());
        entity.setCalificacion(resena.getCalificacion().estrellas());
        entity.setComentario(resena.getComentario());
        entity.setFechaCreacion(resena.getFechaCreacion());
        entity.setRespuestaVendedor(resena.getRespuestaVendedor());
        entity.setFechaRespuesta(resena.getFechaRespuesta());
        return entity;
    }

    public Resena toDomain(ResenaEntity entity) {
        return Resena.reconstruir(
                entity.getId(),
                entity.getIdCompra(),
                entity.getIdProducto(),
                entity.getIdUsuario(),
                new Calificacion(entity.getCalificacion()),
                entity.getComentario(),
                entity.getFechaCreacion(),
                entity.getRespuestaVendedor(),
                entity.getFechaRespuesta()
        );
    }
}