package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.CuponResponse;
import com.uniquindio.backend.domain.entity.Cupon;
import org.springframework.stereotype.Component;

@Component
public class CuponMapper {

    public CuponResponse toResponse(Cupon cupon) {
        return new CuponResponse(
                cupon.getId(),
                cupon.getCodigo(),
                cupon.getPorcentaje(),
                cupon.getVence(),
                cupon.getUsosMaximos(),
                cupon.getUsos(),
                cupon.getVendedorId()
        );
    }
}
