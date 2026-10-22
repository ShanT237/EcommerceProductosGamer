package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ProductoEntity;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Gama;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface ProductoJpaDataRepository extends JpaRepository<ProductoEntity, UUID> {

    @Query(
            value = """
                    select p from ProductoEntity p
                    left join PreventaEntity pv on pv.productoId = p.id
                    where p.eliminado = false
                      and p.pausado = false
                      and (:texto is null or lower(p.nombre) like lower(concat('%', :texto, '%'))
                           or lower(p.descripcion) like lower(concat('%', :texto, '%')))
                      and (:categoria is null or lower(p.categoria) = lower(:categoria))
                      and (:gama is null or p.gama = :gama)
                      and (:precioMin is null or p.precio >= :precioMin)
                      and (:precioMax is null or p.precio <= :precioMax)
                      and (:soloPreventa = false
                           or (pv.id is not null and pv.fechaLanzamiento > :hoy))
                    order by p.nombre asc
                    """,
            countQuery = """
                    select count(distinct p.id) from ProductoEntity p
                    left join PreventaEntity pv on pv.productoId = p.id
                    where p.eliminado = false
                      and p.pausado = false
                      and (:texto is null or lower(p.nombre) like lower(concat('%', :texto, '%'))
                           or lower(p.descripcion) like lower(concat('%', :texto, '%')))
                      and (:categoria is null or lower(p.categoria) = lower(:categoria))
                      and (:gama is null or p.gama = :gama)
                      and (:precioMin is null or p.precio >= :precioMin)
                      and (:precioMax is null or p.precio <= :precioMax)
                      and (:soloPreventa = false
                           or (pv.id is not null and pv.fechaLanzamiento > :hoy))
                    """
    )
    Page<ProductoEntity> buscarCatalogo(
            @Param("texto") String texto,
            @Param("categoria") String categoria,
            @Param("gama") Gama gama,
            @Param("precioMin") Double precioMin,
            @Param("precioMax") Double precioMax,
            @Param("soloPreventa") boolean soloPreventa,
            @Param("hoy") LocalDate hoy,
            Pageable pageable);

    @Query("""
            select p.id as productoId, p.nombre as nombre, sum(c.cantidad) as unidades
            from ProductoEntity p
            join CompraEntity c on c.productoId = p.id
            where c.estado = :estado
              and (:vendedorId is null or p.vendedorId = :vendedorId)
            group by p.id, p.nombre
            order by sum(c.cantidad) desc, p.nombre asc
            """)
    List<ProductoMasVendidoProjection> buscarMasVendidos(
            @Param("vendedorId") UUID vendedorId,
            @Param("estado") EstadoCompra estado,
            Pageable pageable);
}