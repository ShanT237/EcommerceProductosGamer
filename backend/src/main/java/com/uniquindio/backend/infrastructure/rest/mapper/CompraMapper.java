package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ConfirmarCompraResponse;
import com.uniquindio.backend.application.dto.response.RealizarCompraResponse;
import com.uniquindio.backend.domain.entity.Compra;
import org.springframework.stereotype.Component;

@Component
public class CompraMapper {

    public RealizarCompraResponse toRealizarResponse(Compra compra) {
        return RealizarCompraResponse.desde(compra);
    }

    public ConfirmarCompraResponse toConfirmarResponse(Compra compra) {
        return ConfirmarCompraResponse.desde(compra);
    }
}