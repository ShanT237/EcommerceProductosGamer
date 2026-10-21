package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.valueobject.TipoVendedor;

import java.util.UUID;

// Usado en: POST /api/vendedores
public record RegistrarVendedorResponse(
        UUID id,
        String nombre,
        String correo,
        TipoVendedor tipo,
        String documento,
        boolean activo
) {}