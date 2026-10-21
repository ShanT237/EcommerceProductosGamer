package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.WishlistPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WishlistRepositoryJpa implements WishlistRepository {

    private final WishlistJpaDataRepository dataRepository;
    private final WishlistPersistenceMapper mapper;

    @Override
    public Optional<Wishlist> obtenerPorUsuarioId(UUID usuarioId) {
        return dataRepository.findByUsuarioId(usuarioId).map(mapper::toDomain);
    }

    @Override
    public void guardar(Wishlist wishlist) {
        dataRepository.save(mapper.toEntity(wishlist));
    }

    @Override
    public List<Wishlist> obtenerTodasQueContengan(UUID productoId) {
        return dataRepository.findByProductoIdEnProductos(productoId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}