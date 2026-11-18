package com.uniquindio.backend.infrastructure.persistence.jpa.entity;

import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "conversaciones_chat",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_conversacion_chat_contexto",
                columnNames = {"comprador_id", "vendedor_id", "tipo", "referencia_id"}),
        indexes = {
                @Index(name = "idx_conversacion_chat_comprador", columnList = "comprador_id"),
                @Index(name = "idx_conversacion_chat_vendedor", columnList = "vendedor_id")
        })
@Getter @Setter
@NoArgsConstructor
public class ConversacionChatEntity {

    @Id
    private UUID id;

    @Column(name = "comprador_id", nullable = false)
    private UUID compradorId;

    @Column(name = "vendedor_id", nullable = false)
    private UUID vendedorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null")
    private TipoContextoChat tipo;

    @Column(name = "referencia_id", nullable = false)
    private UUID referenciaId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "ultima_actividad", nullable = false)
    private LocalDateTime ultimaActividad;

    @Column(name = "vista_previa", length = 80)
    private String vistaPrevia;
}
