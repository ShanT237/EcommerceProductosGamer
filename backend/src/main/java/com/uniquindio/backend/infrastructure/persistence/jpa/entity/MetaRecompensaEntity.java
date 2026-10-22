package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "metas_recompensa")
@Getter @Setter
@NoArgsConstructor
public class MetaRecompensaEntity {

    @Id
    private UUID id;

    @Column(name = "compras_requeridas", nullable = false)
    private int comprasRequeridas;

    @Column(name = "producto_regalo_id", nullable = false)
    private UUID productoRegaloId;

    @Column(nullable = false)
    private boolean activa;
}