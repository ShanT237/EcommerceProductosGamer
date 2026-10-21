package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.MovimientoPuntosResponse;
import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import org.springframework.stereotype.Component;

@Component
public class MovimientoPuntosMapper {

    public MovimientoPuntosResponse toResponse(MovimientoPuntos movimiento) {
        return MovimientoPuntosResponse.desde(movimiento);
    }
}