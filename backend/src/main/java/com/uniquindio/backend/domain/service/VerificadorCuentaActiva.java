package com.uniquindio.backend.domain.service;

/**
 * Puerto: comprueba que la cuenta detrás de una sesión siga habilitada.
 * Un JWT es válido hasta que expira; este puerto permite cortar la sesión en el momento
 * en que la cuenta se da de baja, sin esperar al vencimiento del token.
 */
public interface VerificadorCuentaActiva {

    /** true solo si la cuenta de la sesión existe y no ha sido dada de baja. */
    boolean estaActiva(SesionToken sesion);
}
