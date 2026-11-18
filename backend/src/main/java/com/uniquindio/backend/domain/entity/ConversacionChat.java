package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.AccesoProhibidoException;
import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Conversación de chat entre un comprador y un vendedor sobre un producto o una orden.
 * Solo los dos participantes pueden consultarla o escribir en ella.
 */
@Getter
public class ConversacionChat {

    private static final int LONGITUD_VISTA_PREVIA = 80;

    private final UUID id;
    private final UUID compradorId;
    private final UUID vendedorId;
    private final TipoContextoChat tipo;
    private final UUID referenciaId;
    private final LocalDateTime creadaEn;
    private LocalDateTime ultimaActividad;
    private String vistaPrevia;

    private ConversacionChat(UUID id, UUID compradorId, UUID vendedorId, TipoContextoChat tipo,
                             UUID referenciaId, LocalDateTime creadaEn, LocalDateTime ultimaActividad,
                             String vistaPrevia) {
        this.id = id;
        this.compradorId = compradorId;
        this.vendedorId = vendedorId;
        this.tipo = tipo;
        this.referenciaId = referenciaId;
        this.creadaEn = creadaEn;
        this.ultimaActividad = ultimaActividad;
        this.vistaPrevia = vistaPrevia;
    }

    public static ConversacionChat iniciar(UUID id, UUID compradorId, UUID vendedorId,
                                           TipoContextoChat tipo, UUID referenciaId,
                                           LocalDateTime ahora) {
        Objects.requireNonNull(id, "El id es obligatorio");
        Objects.requireNonNull(compradorId, "El comprador es obligatorio");
        Objects.requireNonNull(vendedorId, "El vendedor es obligatorio");
        Objects.requireNonNull(tipo, "El tipo de contexto es obligatorio");
        Objects.requireNonNull(referenciaId, "La referencia es obligatoria");
        Objects.requireNonNull(ahora, "La fecha es obligatoria");
        return new ConversacionChat(id, compradorId, vendedorId, tipo, referenciaId, ahora, ahora, null);
    }

    public static ConversacionChat reconstruir(UUID id, UUID compradorId, UUID vendedorId,
                                               TipoContextoChat tipo, UUID referenciaId,
                                               LocalDateTime creadaEn, LocalDateTime ultimaActividad,
                                               String vistaPrevia) {
        return new ConversacionChat(id, compradorId, vendedorId, tipo, referenciaId,
                creadaEn, ultimaActividad, vistaPrevia);
    }

    public boolean esParticipante(UUID usuarioId) {
        return usuarioId != null && (compradorId.equals(usuarioId) || vendedorId.equals(usuarioId));
    }

    public void asegurarParticipante(UUID usuarioId) {
        if (!esParticipante(usuarioId)) {
            throw new AccesoProhibidoException("No participas en esta conversación");
        }
    }

    /** El otro participante de la conversación. */
    public UUID destinatarioDe(UUID remitenteId) {
        asegurarParticipante(remitenteId);
        return compradorId.equals(remitenteId) ? vendedorId : compradorId;
    }

    public void registrarActividad(String contenido, LocalDateTime cuando) {
        this.ultimaActividad = cuando;
        if (contenido == null) {
            this.vistaPrevia = null;
        } else {
            this.vistaPrevia = contenido.length() > LONGITUD_VISTA_PREVIA
                    ? contenido.substring(0, LONGITUD_VISTA_PREVIA)
                    : contenido;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConversacionChat that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
