package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Un regalo no guarda su vendedor: el dueño es el del producto o el del combo al que apunta.
 * Lo usan EnviarRegalo y ConsultarRegalosPorEnviar para que cada vendedor solo gestione lo suyo.
 */
final class DuenoDeRegalo {

    private static final String NOMBRE_GENERICO = "regalo";

    private DuenoDeRegalo() {
    }

    static boolean esDelVendedor(Regalo regalo, UUID vendedorId,
                                 ProductoRepository productoRepository, ComboRepository comboRepository) {
        return vendedorDe(regalo, productoRepository, comboRepository)
                .map(vendedorId::equals)
                .orElse(false);
    }

    /** Vendedor del producto o del combo del regalo; vacío si el destino ya no existe. */
    static Optional<UUID> vendedorDe(Regalo regalo,
                                     ProductoRepository productoRepository, ComboRepository comboRepository) {
        if (regalo.getIdProducto() != null) {
            return productoRepository.obtenerPorId(regalo.getIdProducto()).map(Producto::getVendedorId);
        }
        if (regalo.getIdCombo() != null) {
            return comboRepository.obtenerPorId(regalo.getIdCombo()).map(Combo::getVendedorId);
        }
        return Optional.empty();
    }

    /** Nombre del producto o del combo del regalo; {@code "regalo"} si ya no existe. */
    static String nombreDe(Regalo regalo,
                           ProductoRepository productoRepository, ComboRepository comboRepository) {
        if (regalo.getIdProducto() != null) {
            return productoRepository.obtenerPorId(regalo.getIdProducto())
                    .map(Producto::getNombre).orElse(NOMBRE_GENERICO);
        }
        if (regalo.getIdCombo() != null) {
            return comboRepository.obtenerPorId(regalo.getIdCombo())
                    .map(Combo::getNombre).orElse(NOMBRE_GENERICO);
        }
        return NOMBRE_GENERICO;
    }
}
