package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.CuponPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CuponRepositoryJpa implements CuponRepository {

    private final CuponJpaDataRepository dataRepository;
    private final CuponPersistenceMapper mapper;

    @Override
    public void guardar(Cupon cupon) {
        dataRepository.save(mapper.toEntity(cupon));
    }

    @Override
    public Optional<Cupon> obtenerPorCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        // Mismo criterio que el repo en memoria: el código se guarda en mayúsculas y sin espacios
        return dataRepository.findByCodigo(codigo.trim().toUpperCase()).map(mapper::toDomain);
    }
}