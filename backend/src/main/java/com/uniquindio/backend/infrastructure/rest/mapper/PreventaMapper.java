package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.PreventaResponse;
import com.uniquindio.backend.domain.entity.Preventa;
import org.springframework.stereotype.Component;

@Component
public class PreventaMapper {

    public PreventaResponse toResponse(Preventa preventa) {
        return PreventaResponse.desde(preventa);
    }
}