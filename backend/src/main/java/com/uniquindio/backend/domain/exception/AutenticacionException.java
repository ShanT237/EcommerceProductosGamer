package com.uniquindio.backend.domain.exception;

/**
 * Credenciales inválidas. Mensaje genérico a propósito: no revela si falló el correo o la contraseña.
 */
public class AutenticacionException extends RuntimeException {
    public AutenticacionException(String message) {
        super(message);
    }
}
