package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Override
    public List<Vendedor> obtenerTodos() {
        return new ArrayList<>(vendedores.values());
    }

    @Override
    public Optional<Vendedor> obtenerPorCorreo(String correo) {
        if (correo == null) {
            return Optional.empty();
        }
        return vendedores.values().stream()
                .filter(v -> v.getCorreo().equalsIgnoreCase(correo.trim()))
                .findFirst();
    }

    @Override
    public boolean existeConCorreo(String correo) {
        return obtenerPorCorreo(correo).isPresent();
    }
}
