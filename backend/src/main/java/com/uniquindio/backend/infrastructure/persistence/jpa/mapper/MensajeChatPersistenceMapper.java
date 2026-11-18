package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.MensajeChat;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MensajeChatEntity;
import org.springframework.stereotype.Component;

@Component
public class MensajeChatPersistenceMapper {

    public MensajeChatEntity toEntity(MensajeChat m) {
        MensajeChatEntity e = new MensajeChatEntity();
        e.setId(m.getId());
        e.setConversacionId(m.getConversacionId());
        e.setRemitenteId(m.getRemitenteId());
        e.setClienteId(m.getClienteId());
        e.setContenido(m.getContenido());
        e.setEnviadoEn(m.getEnviadoEn());
        e.setLeidoEn(m.getLeidoEn());
        return e;
    }

    public MensajeChat toDomain(MensajeChatEntity e) {
        return MensajeChat.reconstruir(e.getId(), e.getConversacionId(), e.getRemitenteId(),
                e.getClienteId(), e.getContenido(), e.getEnviadoEn(), e.getLeidoEn());
    }
}
