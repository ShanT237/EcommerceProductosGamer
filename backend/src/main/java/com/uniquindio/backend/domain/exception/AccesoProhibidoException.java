package com.uniquindio.backend.domain.exception;

/** El usuario autenticado intenta acceder a un recurso que no le pertenece (se traduce a 403). */
public class AccesoProhibidoException extends RuntimeException {
    public AccesoProhibidoException(String mensaje) {
        super(mensaje);
    }
}
