package com.uniquindio.backend.domain.entity;

/**
 * Value object (temporalmente en el paquete entity, se moverá luego a valueobject):
 * representa los estados del ciclo de vida de un Regalo.
 */
public enum EstadoRegalo {
    DISPONIBLE,
    ELEGIDO,
    ENVIADO,
    ENTREGADO,
    CANCELADO
}