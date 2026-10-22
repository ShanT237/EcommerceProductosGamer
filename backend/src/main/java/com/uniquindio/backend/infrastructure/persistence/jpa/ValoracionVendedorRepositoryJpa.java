package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.repository.ValoracionVendedorRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ValoracionVendedorPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ValoracionVendedorRepositoryJpa implements ValoracionVendedorRepository {

    private final ValoracionVendedorJpaDataRepository dataRepository;
    private final ValoracionVendedorPersistenceMapper mapper;

    @Override
    public void guardar(ValoracionVendedor valoracion) {
        dataRepository.save(mapper.toEntity(valoracion));
    }

    @Override
    public boolean existePorCompra(UUID compraId) {
        return dataRepository.existsByCompraId(compraId);
    }

    @Override
    public List<ValoracionVendedor> obtenerPorVendedorId(UUID vendedorId) {
        return dataRepository.findByVendedorId(vendedorId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}