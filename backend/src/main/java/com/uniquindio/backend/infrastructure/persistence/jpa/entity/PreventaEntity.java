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
@Table(name = "preventas")
@Getter @Setter
@NoArgsConstructor
public class PreventaEntity {

    @Id
    private UUID id;

    @Column(name = "producto_id", nullable = false, unique = true)
    private UUID productoId;

    @Column(name = "fecha_lanzamiento", nullable = false)
    private LocalDate fechaLanzamiento;

    @Column(nullable = false, length = 500)
    private String descripcion;
}