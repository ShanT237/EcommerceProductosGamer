package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.PromocionResponse;
import com.uniquindio.backend.domain.entity.Promocion;
import org.springframework.stereotype.Component;

@Component
public class PromocionMapper {

    public PromocionResponse toResponse(Promocion promocion) {
        return PromocionResponse.desde(promocion);
    }
}