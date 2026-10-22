package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.PromocionPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PromocionRepositoryJpa implements PromocionRepository {

    private final PromocionJpaDataRepository dataRepository;
    private final PromocionPersistenceMapper mapper;

    @Override
    public void guardar(Promocion promocion) {
        dataRepository.save(mapper.toEntity(promocion));
    }

    @Override
    public Optional<Promocion> obtenerVigentePorProductoId(UUID productoId, LocalDate fecha) {
        return dataRepository.findVigentes(productoId, fecha).stream()
                .findFirst()
                .map(mapper::toDomain);
    }

    @Override
    public List<Promocion> obtenerPorProductoId(UUID productoId) {
        return dataRepository.findByProductoId(productoId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}