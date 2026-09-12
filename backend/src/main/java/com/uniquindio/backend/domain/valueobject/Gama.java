package com.uniquindio.backend.domain.valueobject;

public enum Gama {
    BAJA(1, 6),
    MEDIA(2, 12),
    ALTA(3, 24);

    private final int nivel;
    private final int mesesGarantia;

    Gama(int nivel, int mesesGarantia) {
        this.nivel = nivel;
        this.mesesGarantia = mesesGarantia;
    }

    public boolean esSuperiorA(Gama otra) {
        return this.nivel > otra.nivel;
    }

    public int mesesGarantia() {
        return mesesGarantia;
    }
}