package com.uniquindio.backend.domain.valueobject;

import com.uniquindio.backend.domain.exception.ReglaDominioException;

/**
 * Value object Precio.
 *  - Invariante: el precio debe ser siempre positivo (no puede ser cero ni negativo).
 *  - Inmutable: cualquier operación retorna un nuevo Precio, nunca modifica el existente.
 */
public record Precio(double valor) {

    public Precio {
        if (valor <= 0) {
            throw new ReglaDominioException("El precio debe ser positivo");
        }
    }

    /**
     * Aplica un descuento porcentual (0-100) y retorna un nuevo Precio con el valor resultante.
     */
    public Precio aplicarDescuento(double porcentaje) {
        if (porcentaje < 0 || porcentaje >= 100) {
            throw new ReglaDominioException("El porcentaje de descuento debe estar entre 0 y 100");
        }
        return new Precio(this.valor - (this.valor * porcentaje / 100));
    }

    public Precio sumar(Precio otro) {
        return new Precio(this.valor + otro.valor);
    }
}