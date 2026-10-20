package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.CompraPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompraRepositoryJpa implements CompraRepository {

    // Una compra está "activa" si está pendiente o completada
    private static final List<EstadoCompra> ESTADOS_ACTIVOS =
            List.of(EstadoCompra.PENDIENTE, EstadoCompra.COMPLETADA);

    private final CompraJpaDataRepository dataRepository;
    private final CompraPersistenceMapper mapper;

    @Override
    public Optional<Compra> obtenerPorId(UUID id) {
        return dataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public void guardar(Compra compra) {
        dataRepository.save(mapper.toEntity(compra));
    }

    @Override
    public List<Compra> obtenerPorUsuarioId(UUID usuarioId) {
        return dataRepository.findByUsuarioId(usuarioId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existeCompraDe(UUID usuarioId, UUID productoId) {
        return dataRepository.existsByUsuarioIdAndProductoIdAndEstadoIn(
                usuarioId, productoId, ESTADOS_ACTIVOS);
    }

    @Override
    public boolean existeCompraActivaDeProducto(UUID productoId) {
        return dataRepository.existsByProductoIdAndEstadoIn(productoId, ESTADOS_ACTIVOS);
    }

    @Override                                                              // <-- NUEVO
    public long contarCompletadasDe(UUID usuarioId) {
        return dataRepository.countByUsuarioIdAndEstado(usuarioId, EstadoCompra.COMPLETADA);
    }

    @Override
    public boolean existeCompraDeAlguno(UUID usuarioId, Collection<UUID> productoIds) {
        if (productoIds.isEmpty()) return false;
        return dataRepository.existsByUsuarioIdAndProductoIdInAndEstadoIn(
                usuarioId, productoIds, ESTADOS_ACTIVOS);
    }
}