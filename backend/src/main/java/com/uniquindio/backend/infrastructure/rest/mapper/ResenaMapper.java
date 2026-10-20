package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.SubirResenaResponse;
import com.uniquindio.backend.domain.entity.Resena;
import org.springframework.stereotype.Component;

@Component
public class ResenaMapper {
    public SubirResenaResponse toResponse(Resena resena) {
        return SubirResenaResponse.desde(resena);
    }
}