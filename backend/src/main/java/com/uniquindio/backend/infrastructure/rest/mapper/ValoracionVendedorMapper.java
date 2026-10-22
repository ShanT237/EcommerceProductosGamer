package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ValoracionVendedorResponse;
import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import org.springframework.stereotype.Component;

@Component
public class ValoracionVendedorMapper {

    public ValoracionVendedorResponse toResponse(ValoracionVendedor valoracion) {
        return ValoracionVendedorResponse.desde(valoracion);
    }
}