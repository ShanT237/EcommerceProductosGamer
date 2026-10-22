package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.HashContrasena;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt (costo 10) con sal aleatoria incluida en el propio hash.
 * Implementa el puerto HashContrasena: el dominio no conoce el algoritmo.
 */
@Component
public class BCryptHashContrasena implements HashContrasena {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String hashear(String plana) {
        return encoder.encode(plana);
    }

    @Override
    public boolean coincide(String plana, String hashAlmacenado) {
        if (plana == null || hashAlmacenado == null) {
            return false;
        }
        return encoder.matches(plana, hashAlmacenado);
    }
}