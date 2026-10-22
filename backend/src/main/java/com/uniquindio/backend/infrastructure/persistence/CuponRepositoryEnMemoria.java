package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CuponRepositoryEnMemoria implements CuponRepository {

    private final Map<String, Cupon> cupones = new HashMap<>();

    @Override
    public void guardar(Cupon cupon) {
        cupones.put(cupon.getCodigo(), cupon);
    }

    @Override
    public Optional<Cupon> obtenerPorCodigo(String codigo) {
        return codigo == null ? Optional.empty()
                : Optional.ofNullable(cupones.get(codigo.trim().toUpperCase()));
    }
}
