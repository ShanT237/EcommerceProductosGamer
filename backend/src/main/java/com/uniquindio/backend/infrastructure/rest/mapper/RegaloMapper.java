package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.RegaloResponse;
import com.uniquindio.backend.domain.entity.Regalo;
import org.springframework.stereotype.Component;

@Component
public class RegaloMapper {

    public RegaloResponse toResponse(Regalo regalo) {
        return RegaloResponse.desde(regalo);
    }
}