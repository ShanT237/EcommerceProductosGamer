package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.domain.entity.Combo;
import org.springframework.stereotype.Component;

@Component
public class ComboMapper {
    public ComboResponse toResponse(Combo combo) {
        return ComboResponse.desde(combo);
    }
}