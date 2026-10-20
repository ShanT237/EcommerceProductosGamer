package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.VendedorPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class VendedorRepositoryJpa implements VendedorRepository {

    private final VendedorJpaDataRepository dataRepository;
    private final VendedorPersistenceMapper mapper;

    @Override
    public Optional<Vendedor> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Vendedor vendedor) {
        dataRepository.save(mapper.toEntity(vendedor));
    }

    @Override
    public boolean existeConDocumento(String documento) {
        return dataRepository.existsByDocumento(documento);
    }
}