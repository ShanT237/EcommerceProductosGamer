package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Compra;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Compra.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar compras
 */
public interface CompraRepository {

    Optional<Compra> obtenerPorId(UUID id);

    void guardar(Compra compra);

    /**
     * Indica si el usuario ya tiene una compra registrada de ese producto.
     * La usa RealizarCompraUseCase para poder validar la Regla 9
     * (un producto exclusivo solo se compra una vez por usuario) sin que
     * el caso de uso tenga que decidir nada por su cuenta.
     */
    boolean existeCompraDe(UUID usuarioId, UUID productoId);
}