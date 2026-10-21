package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ProductoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductoRepositoryJpa implements ProductoRepository {

    private final ProductoJpaDataRepository dataRepository;
    private final ProductoPersistenceMapper mapper;

    @Override
    public Optional<Producto> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Producto producto) {
        dataRepository.save(mapper.toEntity(producto));
    }

    @Override
    public List<Producto> obtenerTodos() {
        return dataRepository.findAll().stream().map(mapper::toDomain).toList();
    }
}