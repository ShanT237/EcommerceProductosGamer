package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Cupon;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CuponRepository {
    void guardar(Cupon cupon);
    Optional<Cupon> obtenerPorCodigo(String codigo);
    List<Cupon> obtenerPorVendedorId(UUID vendedorId);
    boolean existePorCodigo(String codigo);

    /** ¿Este comprador ya usó este cupón en algún pedido? (un uso por comprador) */
    boolean fueUsadoPor(UUID cuponId, UUID usuarioId);

    /** Deja constancia de que el comprador usó el cupón. */
    void registrarUsoDe(UUID cuponId, UUID usuarioId);
}
