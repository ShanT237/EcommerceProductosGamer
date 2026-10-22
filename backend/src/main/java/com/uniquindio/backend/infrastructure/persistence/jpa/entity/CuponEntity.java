package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "cupones")
@Getter @Setter
@NoArgsConstructor
public class CuponEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false)
    private double porcentaje;

    @Column(nullable = false)
    private LocalDate vence;

    @Column(name = "usos_maximos", nullable = false)
    private int usosMaximos;

    @Column(nullable = false)
    private int usos;
}