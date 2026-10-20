package com.uniquindio.backend.domain.service;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto: emisión y validación de tokens de sesión.
 */
public interface EmisorToken {
    String emitir(UUID usuarioId, String correo);

    /** Devuelve el id del usuario si el token es auténtico y no ha expirado. */
    Optional<UUID> validar(String token);
}
