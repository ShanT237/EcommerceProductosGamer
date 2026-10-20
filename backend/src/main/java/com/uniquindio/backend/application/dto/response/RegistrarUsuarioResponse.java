package com.uniquindio.backend.application.dto.response;

import java.util.UUID;

// Usado en: POST /api/usuarios
public record RegistrarUsuarioResponse(
        UUID id,
        String nombre,
        String correo,
        int puntos,
        boolean activo
) {}