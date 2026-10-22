package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CatalogoRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.FiltroProducto;
import com.uniquindio.backend.domain.valueobject.Pagina;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ProductoPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductoRepositoryJpa implements ProductoRepository, CatalogoRepository {

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

    @Override
    public Pagina<Producto> buscar(FiltroProducto filtro, int pagina, int tamano, LocalDate hoy) {
        Page<ProductoEntity> resultados = dataRepository.buscarCatalogo(
                normalizar(filtro.texto()),
                normalizar(filtro.categoria()),
                filtro.gama(),
                filtro.precioMin(),
                filtro.precioMax(),
                filtro.soloPreventa(),
                hoy,
                PageRequest.of(pagina, tamano));

        return new Pagina<>(
                resultados.getContent().stream().map(mapper::toDomain).toList(),
                resultados.getNumber(),
                resultados.getSize(),
                resultados.getTotalElements(),
                resultados.getTotalPages());
    }

    private String normalizar(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}