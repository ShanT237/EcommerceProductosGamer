package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Mensaje de una conversación de chat. El {@code clienteId} lo genera el cliente y, junto con el
 * remitente, hace idempotente el reenvío (reintentos tras una caída de red).
 */
@Getter
public class MensajeChat {

    public static final int LONGITUD_MAXIMA = 2000;

    private final UUID id;
    private final UUID conversacionId;
    private final UUID remitenteId;
    private final UUID clienteId;
    private final String contenido;
    private final LocalDateTime enviadoEn;
    private final LocalDateTime leidoEn;

    private MensajeChat(UUID id, UUID conversacionId, UUID remitenteId, UUID clienteId,
                        String contenido, LocalDateTime enviadoEn, LocalDateTime leidoEn) {
        this.id = id;
        this.conversacionId = conversacionId;
        this.remitenteId = remitenteId;
        this.clienteId = clienteId;
        this.contenido = contenido;
        this.enviadoEn = enviadoEn;
        this.leidoEn = leidoEn;
    }

    public static MensajeChat enviar(UUID id, UUID conversacionId, UUID remitenteId, UUID clienteId,
                                     String contenido, LocalDateTime ahora) {
        Objects.requireNonNull(id, "El id es obligatorio");
        Objects.requireNonNull(conversacionId, "La conversación es obligatoria");
        Objects.requireNonNull(remitenteId, "El remitente es obligatorio");
        Objects.requireNonNull(clienteId, "El clienteId es obligatorio");
        Objects.requireNonNull(ahora, "La fecha es obligatoria");
        String limpio = contenido == null ? "" : contenido.trim();
        if (limpio.isEmpty()) {
            throw new ReglaDominioException("El mensaje no puede estar vacío");
        }
        if (limpio.length() > LONGITUD_MAXIMA) {
            throw new ReglaDominioException("El mensaje no puede superar " + LONGITUD_MAXIMA + " caracteres");
        }
        // MariaDB guarda microsegundos: truncar aquí mantiene el cursor (enviadoEn, id) consistente.
        return new MensajeChat(id, conversacionId, remitenteId, clienteId, limpio,
                ahora.truncatedTo(ChronoUnit.MICROS), null);
    }

    public static MensajeChat reconstruir(UUID id, UUID conversacionId, UUID remitenteId, UUID clienteId,
                                          String contenido, LocalDateTime enviadoEn, LocalDateTime leidoEn) {
        return new MensajeChat(id, conversacionId, remitenteId, clienteId, contenido, enviadoEn, leidoEn);
    }

    public boolean estaLeido() {
        return leidoEn != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MensajeChat that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
