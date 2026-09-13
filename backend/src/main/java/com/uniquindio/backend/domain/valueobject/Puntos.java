package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

public record Puntos(int valor) {

    public Puntos {
        if (valor < 0) {
            throw new ReglaDominioException("Los puntos no pueden ser negativos");
        }
    }

    public Puntos sumar(Puntos otros) {
        return new Puntos(this.valor + otros.valor);
    }

    public static Puntos cero() {
        return new Puntos(0);
    }
}