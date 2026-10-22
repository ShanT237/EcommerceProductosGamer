package com.uniquindio.backend.domain.service;

/**
 * Puerto: cómo se protege una contraseña. El dominio no conoce el algoritmo.
 */
public interface HashContrasena {
    String hashear(String plana);
    boolean coincide(String plana, String hashAlmacenado);
}
