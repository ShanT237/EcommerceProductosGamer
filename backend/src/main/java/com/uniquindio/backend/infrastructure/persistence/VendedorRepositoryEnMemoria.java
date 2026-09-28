package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VendedorRepositoryEnMemoria implements VendedorRepository {

    private final Map<UUID, Vendedor> vendedores = new HashMap<>();

    @Override
    public Optional<Vendedor> obtenerPorId(UUID id) {
        return Optional.ofNullable(vendedores.get(id));
    }

    @Override
    public void guardar(Vendedor vendedor) {
        vendedores.put(vendedor.getId(), vendedor);
    }

    @Override
    public boolean existeConDocumento(String documento) {
        return vendedores.values().stream()
                .anyMatch(v -> v.getDocumento().equals(documento));
    }
}