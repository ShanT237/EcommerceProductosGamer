package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

/**
 * Regla de contraseña segura: mínimo 8 caracteres, con al menos una mayúscula y un número.
 * Solo valida la contraseña en claro; nunca la guarda.
 */
public final class Contrasena {

    private static final int LONGITUD_MINIMA = 8;

    private Contrasena() {
    }

    public static void validarFortaleza(String plana) {
        if (plana == null || plana.length() < LONGITUD_MINIMA) {
            throw new ReglaDominioException(
                    "La contraseña debe tener mínimo " + LONGITUD_MINIMA + " caracteres");
        }
        if (plana.chars().noneMatch(Character::isUpperCase)) {
            throw new ReglaDominioException("La contraseña debe tener al menos una mayúscula");
        }
        if (plana.chars().noneMatch(Character::isDigit)) {
            throw new ReglaDominioException("La contraseña debe tener al menos un número");
        }
    }
}
