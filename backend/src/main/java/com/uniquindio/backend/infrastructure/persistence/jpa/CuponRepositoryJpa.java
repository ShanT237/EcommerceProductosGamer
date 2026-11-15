package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CuponUsoEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.CuponPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CuponRepositoryJpa implements CuponRepository {

    private final CuponJpaDataRepository dataRepository;
    private final CuponUsoJpaDataRepository usoDataRepository;
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

    @Override
    public List<Cupon> obtenerPorVendedorId(UUID vendedorId) {
        return dataRepository.findByVendedorId(vendedorId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return codigo != null && dataRepository.existsByCodigo(codigo.trim().toUpperCase());
    }

    @Override
    public boolean fueUsadoPor(UUID cuponId, UUID usuarioId) {
        return usoDataRepository.existsByCuponIdAndUsuarioId(cuponId, usuarioId);
    }

    @Override
    public void registrarUsoDe(UUID cuponId, UUID usuarioId) {
        if (usoDataRepository.existsByCuponIdAndUsuarioId(cuponId, usuarioId)) {
            return;
        }
        CuponUsoEntity uso = new CuponUsoEntity();
        uso.setId(UUID.randomUUID());
        uso.setCuponId(cuponId);
        uso.setUsuarioId(usuarioId);
        usoDataRepository.save(uso);
    }
}
