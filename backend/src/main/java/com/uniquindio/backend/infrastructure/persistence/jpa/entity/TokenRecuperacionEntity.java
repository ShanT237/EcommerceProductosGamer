package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_recuperacion",
        indexes = @Index(name = "idx_tokens_recuperacion_cuenta", columnList = "cuenta_id"))
@Getter @Setter
@NoArgsConstructor
public class TokenRecuperacionEntity {

    @Id
    private UUID id;

    @Column(name = "hash_token", nullable = false, unique = true, length = 64)
    private String hashToken;

    @Column(name = "cuenta_id", nullable = false)
    private UUID cuentaId;

    @Column(nullable = false, length = 20)
    private String rol;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;
}
