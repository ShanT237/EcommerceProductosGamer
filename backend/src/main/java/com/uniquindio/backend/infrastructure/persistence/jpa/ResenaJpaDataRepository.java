package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ResenaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

interface ResenaJpaDataRepository extends JpaRepository<ResenaEntity, String> {

    boolean existsByIdUsuarioAndIdProducto(String idUsuario, String idProducto);

    List<ResenaEntity> findByIdProducto(String idProducto);

    @Query("""
            select r.idProducto as productoId, avg(r.calificacion) as promedio
            from ResenaEntity r
            where r.idProducto in :productoIds
            group by r.idProducto
            """)
    List<PromedioResenaProjection> promediosPorProducto(@Param("productoIds") List<String> productoIds);
}