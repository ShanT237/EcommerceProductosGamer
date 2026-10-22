package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.repository.ValoracionVendedorRepository;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ValoracionVendedorRepositoryEnMemoria implements ValoracionVendedorRepository {

    private final Map<UUID, ValoracionVendedor> valoraciones = new HashMap<>();

    @Override
    public void guardar(ValoracionVendedor valoracion) {
        valoraciones.put(valoracion.getId(), valoracion);
    }

    @Override
    public boolean existePorCompra(UUID compraId) {
        return valoraciones.values().stream().anyMatch(v -> v.getCompraId().equals(compraId));
    }

    @Override
    public List<ValoracionVendedor> obtenerPorVendedorId(UUID vendedorId) {
        return valoraciones.values().stream()
                .filter(v -> v.getVendedorId().equals(vendedorId))
                .toList();
    }
}
