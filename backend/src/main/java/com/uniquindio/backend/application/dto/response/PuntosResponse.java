package com.uniquindio.backend.application.dto.response;

import com.uniquindio.backend.domain.entity.Usuario;

import java.util.UUID;

public record PuntosResponse(
        UUID usuarioId,
        int saldo
) {
    public static PuntosResponse desde(Usuario usuario) {
        return new PuntosResponse(usuario.getId(), usuario.getPuntos());
    }
}
