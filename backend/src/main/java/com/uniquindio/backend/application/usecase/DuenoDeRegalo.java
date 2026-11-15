package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;

import java.util.UUID;

/**
 * Un regalo no guarda su vendedor: el dueño es el del producto o el del combo al que apunta.
 * Lo usan EnviarRegalo y ConsultarRegalosPorEnviar para que cada vendedor solo gestione lo suyo.
 */
final class DuenoDeRegalo {

    private DuenoDeRegalo() {
    }

    static boolean esDelVendedor(Regalo regalo, UUID vendedorId,
                                 ProductoRepository productoRepository, ComboRepository comboRepository) {
        if (regalo.getIdProducto() != null) {
            return productoRepository.obtenerPorId(regalo.getIdProducto())
                    .map(producto -> vendedorId.equals(producto.getVendedorId()))
                    .orElse(false);
        }
        if (regalo.getIdCombo() != null) {
            return comboRepository.obtenerPorId(regalo.getIdCombo())
                    .map(combo -> vendedorId.equals(combo.getVendedorId()))
                    .orElse(false);
        }
        return false;
    }
}
