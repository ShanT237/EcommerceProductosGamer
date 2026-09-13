package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

public record Unidades(int cantidad) {

    public Unidades {
        if (cantidad <= 0) {
            throw new ReglaDominioException("Las unidades deben ser mayores a cero");
        }
    }

    public Unidades restar(int cantidadARestar) {
        return new Unidades(this.cantidad - cantidadARestar);
    }
}