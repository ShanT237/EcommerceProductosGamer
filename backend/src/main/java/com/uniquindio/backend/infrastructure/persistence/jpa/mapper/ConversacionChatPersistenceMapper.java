package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ConversacionChatEntity;
import org.springframework.stereotype.Component;

@Component
public class ConversacionChatPersistenceMapper {

    public ConversacionChatEntity toEntity(ConversacionChat c) {
        ConversacionChatEntity e = new ConversacionChatEntity();
        e.setId(c.getId());
        e.setCompradorId(c.getCompradorId());
        e.setVendedorId(c.getVendedorId());
        e.setTipo(c.getTipo());
        e.setReferenciaId(c.getReferenciaId());
        e.setCreadaEn(c.getCreadaEn());
        e.setUltimaActividad(c.getUltimaActividad());
        e.setVistaPrevia(c.getVistaPrevia());
        return e;
    }

    public ConversacionChat toDomain(ConversacionChatEntity e) {
        return ConversacionChat.reconstruir(e.getId(), e.getCompradorId(), e.getVendedorId(),
                e.getTipo(), e.getReferenciaId(), e.getCreadaEn(), e.getUltimaActividad(),
                e.getVistaPrevia());
    }
}
