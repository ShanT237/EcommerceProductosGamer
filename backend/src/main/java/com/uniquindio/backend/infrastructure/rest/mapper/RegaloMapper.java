package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ElegirRegaloResponse;
import com.uniquindio.backend.domain.entity.Regalo;
import org.springframework.stereotype.Component;

@Component
public class RegaloMapper {

    public ElegirRegaloResponse toElegirResponse(Regalo regalo) {
        return new ElegirRegaloResponse(
                regalo.getId(),
                regalo.getIdUsuario(),
                regalo.getIdProducto(),
                regalo.getIdCombo(),
                regalo.getEstado()
        );
    }
}