package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mensajes_chat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mensaje_chat_remitente_cliente",
                columnNames = {"remitente_id", "cliente_id"}),
        indexes = @Index(name = "idx_mensaje_chat_conversacion_fecha",
                columnList = "conversacion_id, enviado_en, id"))
@Getter @Setter
@NoArgsConstructor
public class MensajeChatEntity {

    @Id
    private UUID id;

    @Column(name = "conversacion_id", nullable = false)
    private UUID conversacionId;

    @Column(name = "remitente_id", nullable = false)
    private UUID remitenteId;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(nullable = false, length = 2000)
    private String contenido;

    @Column(name = "enviado_en", nullable = false)
    private LocalDateTime enviadoEn;

    @Column(name = "leido_en")
    private LocalDateTime leidoEn;
}
