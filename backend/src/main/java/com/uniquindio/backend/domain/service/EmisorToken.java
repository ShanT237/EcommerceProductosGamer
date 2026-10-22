package com.uniquindio.backend.domain.service;

import com.uniquindio.backend.domain.valueobject.Rol;

import java.util.Optional;
import java.util.UUID;

/**
 * Puerto: emisión y validación de tokens de sesión.
 */
public interface EmisorToken {
    String emitir(UUID usuarioId, String correo, Rol rol);

    /** Devuelve la sesión (id y rol) si el token es auténtico y no ha expirado. */
    Optional<SesionToken> validar(String token);
}