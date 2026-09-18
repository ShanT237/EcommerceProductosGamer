package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

/**
 * Value Object: Calificación (1 a 5 estrellas).
 * Se define únicamente por su valor -> record, sin identidad propia.
 * Validación de rango en el constructor (ninguna calificación fuera de 1-5
 * debe poder existir en el sistema).
 */
public record Calificacion(int estrellas) {

    public Calificacion {
        if (estrellas < 1 || estrellas > 5) {
            throw new ReglaDominioException(
                    "La calificación debe estar entre 1 y 5 estrellas, se recibió: " + estrellas);
        }
    }

    public boolean esPositiva() {
        return estrellas >= 4;
    }

    public boolean esNegativa() {
        return estrellas <= 2;
    }
}