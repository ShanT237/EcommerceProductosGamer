package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** Registro de que un comprador ya usó un cupón; la restricción única impide repetirlo. */
@Entity
@Table(name = "cupon_usos",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cupon_id", "usuario_id"}))
@Getter @Setter
@NoArgsConstructor
public class CuponUsoEntity {

    @Id
    private UUID id;

    @Column(name = "cupon_id", nullable = false)
    private UUID cuponId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;
}
