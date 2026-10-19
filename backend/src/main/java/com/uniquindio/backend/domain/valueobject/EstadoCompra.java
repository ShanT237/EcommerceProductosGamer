package com.uniquindio.backend.domain.valueobject;

import java.util.Arrays;

public enum EstadoCompra {
    PENDIENTE,
    COMPLETADA,
    REEMBOLSADA,
    CANCELADA,
    EXPIRADA;

    public boolean puedeTransicionarA(EstadoCompra siguiente) {
        return switch (this) {
            case PENDIENTE  -> siguiente == COMPLETADA
                    || siguiente == CANCELADA
                    || siguiente == EXPIRADA;
            case COMPLETADA -> siguiente == REEMBOLSADA;
            case REEMBOLSADA, CANCELADA, EXPIRADA -> false;   // estados finales
        };
    }

    public boolean esFinal() {
        return Arrays.stream(values()).noneMatch(this::puedeTransicionarA);
    }
}