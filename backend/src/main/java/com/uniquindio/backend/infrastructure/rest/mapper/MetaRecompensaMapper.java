package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.MetaRecompensaResponse;
import com.uniquindio.backend.domain.entity.MetaRecompensa;
import org.springframework.stereotype.Component;

@Component
public class MetaRecompensaMapper {

    public MetaRecompensaResponse toResponse(MetaRecompensa meta) {
        return MetaRecompensaResponse.desde(meta);
    }
}