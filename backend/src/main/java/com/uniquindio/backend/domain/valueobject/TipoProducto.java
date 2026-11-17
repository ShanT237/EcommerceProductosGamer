package com.uniquindio.backend.domain.valueobject;

/**
 * Naturaleza del producto. Solo los DIGITAL habilitan la operación «descargar»
 * (no se entrega ningún archivo: solo se marca la compra como descargada).
 */
public enum TipoProducto {
    DIGITAL,
    FISICO;

    public boolean esDigital() {
        return this == DIGITAL;
    }
}
