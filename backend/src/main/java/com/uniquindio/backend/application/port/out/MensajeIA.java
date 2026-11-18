package com.uniquindio.backend.application.port.out;

import java.util.Objects;

/** Un mensaje de la conversación con el asistente de IA (rol + texto). */
public record MensajeIA(Rol rol, String contenido) {

    public enum Rol {
        SISTEMA,
        USUARIO,
        ASISTENTE
    }

    public MensajeIA {
        Objects.requireNonNull(rol, "El rol del mensaje es obligatorio");
        Objects.requireNonNull(contenido, "El contenido del mensaje es obligatorio");
    }
}
