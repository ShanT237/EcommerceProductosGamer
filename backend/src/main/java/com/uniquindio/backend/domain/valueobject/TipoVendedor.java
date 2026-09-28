package com.uniquindio.backend.domain.valueobject;

/**
 * Value Object: tipo de vendedor.
 * Se define únicamente por su valor. Cada tipo sabe qué documento
 * de identificación le corresponde.
 */
public enum TipoVendedor {
    PERSONA_NATURAL("Cédula"),
    EMPRESA("NIT");

    private final String tipoDocumento;

    TipoVendedor(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public boolean esEmpresa() {
        return this == EMPRESA;
    }

    public String tipoDocumento() {
        return tipoDocumento;
    }
}