package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "regalos")
@Getter @Setter
@NoArgsConstructor
public class RegaloEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRegalo estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "id_producto")
    private UUID idProducto;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "id_combo")
    private UUID idCombo;

    @Column(nullable = false)
    private boolean notificado;
}