package com.uniquindio.backend.domain.exception;

/**
 * Regla de dominio violada por duplicidad (algo que ya existe). Responde 409.
 * Hereda de ReglaDominioException para no romper los catch ni los tests existentes.
 */
public class ConflictoException extends ReglaDominioException {
    public ConflictoException(String message) {
        super(message);
    }
}