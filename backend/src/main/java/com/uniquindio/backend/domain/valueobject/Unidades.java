package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

public record Unidades(int cantidad) {

    public Unidades {
        if (cantidad < 0) {
            throw new ReglaDominioException("Las unidades no pueden ser negativas");
        }
    }

    public Unidades restar(int cantidadARestar) {
        if (cantidadARestar <= 0) {
            throw new ReglaDominioException("La cantidad a restar debe ser positiva");
        }
        if (cantidadARestar > this.cantidad) {
            throw new ReglaDominioException("No hay unidades suficientes");
        }
        return new Unidades(this.cantidad - cantidadARestar);
    }
}