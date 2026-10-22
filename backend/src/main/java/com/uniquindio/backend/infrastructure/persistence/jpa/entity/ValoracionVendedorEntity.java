package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "valoraciones_vendedor")
@Getter @Setter
@NoArgsConstructor
public class ValoracionVendedorEntity {

    @Id
    private UUID id;

    // Una compra solo puede valorarse una vez
    @Column(name = "compra_id", nullable = false, unique = true)
    private UUID compraId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "vendedor_id", nullable = false)
    private UUID vendedorId;

    @Column(nullable = false)
    private int calificacion;

    @Column(nullable = false, length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;
}