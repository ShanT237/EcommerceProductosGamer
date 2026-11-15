package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ComboPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ComboRepositoryJpa implements ComboRepository {

    private final ComboJpaDataRepository dataRepository;
    private final ComboPersistenceMapper mapper;

    @Override
    public Optional<Combo> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Combo combo) {
        dataRepository.save(mapper.toEntity(combo));
    }

    @Override
    public List<Combo> obtenerTodos() {
        return dataRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Combo> obtenerPorVendedorId(UUID vendedorId) {
        return dataRepository.findByVendedorId(vendedorId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void eliminar(UUID id) {
        dataRepository.deleteById(id);
    }
}
