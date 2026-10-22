package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "compras")
@Getter @Setter
@NoArgsConstructor
public class CompraEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false)
    private int cantidad;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "precio_unitario", nullable = false)
    private double precioUnitario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado;

    @Column(nullable = false)
    private boolean resenada;

    @Column(name = "puntos_asignados", nullable = false)
    private boolean puntosAsignados;

    @Column(nullable = false)
    private boolean descargado;

    @Column(name = "cupon_aplicado", nullable = false, columnDefinition = "boolean default false")
    private boolean cuponAplicado;
}