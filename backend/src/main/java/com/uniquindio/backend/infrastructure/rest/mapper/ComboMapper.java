package com.uniquindio.backend.infrastructure.rest.mapper;

import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.application.dto.response.ComprarComboResponse;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ComboMapper {
    public ComboResponse toResponse(Combo combo) {
        return ComboResponse.desde(combo);
    }

    public ComprarComboResponse toComprarResponse(UUID comboId, List<Compra> compras) {
        return ComprarComboResponse.desde(comboId, compras);
    }
}