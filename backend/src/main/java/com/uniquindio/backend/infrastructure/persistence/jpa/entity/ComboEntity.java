package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "combos")
@Getter @Setter
@NoArgsConstructor
public class ComboEntity {

    @Id
    private UUID id;

    @Column(name = "vendedor_id", nullable = false)
    private UUID vendedorId;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(nullable = false)
    private double descuento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "combo_productos", joinColumns = @JoinColumn(name = "combo_id"))
    @Column(name = "producto_id")
    private List<UUID> productos = new ArrayList<>();
}