package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity // Esta clase se mapea a una tabla
@Table(name = "productos")
@Getter @Setter
@NoArgsConstructor
public class ProductoEntity {

    @Id
    private UUID id;

    @Column(name = "vendedor_id", nullable = false)
    private UUID vendedorId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private double precio;

    @Column(name = "precio_original")
    private Double precioOriginal;

    @Column(nullable = false)
    private int stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Gama gama;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Exclusividad exclusividad;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean pausado;
}