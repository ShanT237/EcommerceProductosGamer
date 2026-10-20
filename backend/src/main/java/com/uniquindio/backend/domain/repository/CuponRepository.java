package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Cupon;

import java.util.Optional;

public interface CuponRepository {
    void guardar(Cupon cupon);
    Optional<Cupon> obtenerPorCodigo(String codigo);
}
