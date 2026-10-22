package com.uniquindio.backend.domain.valueobject;

/**
 * Value Object: rol de una cuenta autenticada.
 * Se define únicamente por su valor. Viaja en el token JWT y permite
 * autorizar operaciones según quién las ejecuta.
 */
public enum Rol {
    COMPRADOR,
    VENDEDOR;

    public boolean esComprador() {
        return this == COMPRADOR;
    }

    public boolean esVendedor() {
        return this == VENDEDOR;
    }
}