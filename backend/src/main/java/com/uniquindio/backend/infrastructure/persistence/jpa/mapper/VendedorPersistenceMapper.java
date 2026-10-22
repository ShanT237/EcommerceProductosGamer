package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.VendedorEntity;
import org.springframework.stereotype.Component;

@Component
public class VendedorPersistenceMapper {

    public VendedorEntity toEntity(Vendedor vendedor) {
        VendedorEntity entity = new VendedorEntity();
        entity.setId(vendedor.getId());
        entity.setNombre(vendedor.getNombre());
        entity.setCorreo(vendedor.getCorreo());
        entity.setTipo(vendedor.getTipo());
        entity.setDocumento(vendedor.getDocumento());
        entity.setActivo(vendedor.isActivo());
        entity.setContrasenaHash(vendedor.obtenerHashContrasena());
        return entity;
    }

    public Vendedor toDomain(VendedorEntity entity) {
        return Vendedor.reconstruir(
                entity.getId(),
                entity.getNombre(),
                entity.getCorreo(),
                entity.getTipo(),
                entity.getDocumento(),
                entity.isActivo(),
                entity.getContrasenaHash());
    }
}