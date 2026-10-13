package com.uniquindio.backend.domain.valueobject;

import java.util.Arrays;

public enum EstadoRegalo {
    DISPONIBLE,
    ELEGIDO,
    ENVIADO,
    ENTREGADO,
    CANCELADO;

    public boolean puedeTransicionarA(EstadoRegalo siguiente) {
        return switch (this) {
            case DISPONIBLE -> siguiente == ELEGIDO   || siguiente == CANCELADO;
            case ELEGIDO    -> siguiente == ENVIADO   || siguiente == CANCELADO;
            case ENVIADO    -> siguiente == ENTREGADO || siguiente == CANCELADO;
            case ENTREGADO, CANCELADO -> false;   // estados finales
        };
    }

    public boolean esFinal() {
        return Arrays.stream(values()).noneMatch(this::puedeTransicionarA);
    }
}