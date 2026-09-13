package com.uniquindio.backend.domain.valueobject;

public enum Exclusividad {
    NORMAL,
    EXCLUSIVO;

    public boolean esExclusivo() {
        return this == EXCLUSIVO;
    }
}