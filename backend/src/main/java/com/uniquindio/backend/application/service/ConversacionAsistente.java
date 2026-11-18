package com.uniquindio.backend.application.service;

import com.uniquindio.backend.application.port.out.MensajeIA;

import java.util.ArrayList;
import java.util.List;

/**
 * Arma la conversación que se envía al modelo: un mensaje de sistema (instrucciones + contexto),
 * los últimos turnos del historial y la pregunta actual. Del historial que manda el cliente se
 * descartan los mensajes de sistema: solo el backend decide las instrucciones.
 */
public final class ConversacionAsistente {

    static final int MAX_HISTORIAL = 10;

    private ConversacionAsistente() {
    }

    public static List<MensajeIA> armar(String instrucciones, String contexto,
                                        List<MensajeIA> historial, String mensajeUsuario) {
        List<MensajeIA> previos = historial == null ? List.of() : historial.stream()
                .filter(m -> m.rol() != MensajeIA.Rol.SISTEMA)
                .toList();

        List<MensajeIA> conversacion = new ArrayList<>();
        conversacion.add(new MensajeIA(MensajeIA.Rol.SISTEMA, instrucciones + "\n\n" + contexto));
        conversacion.addAll(previos.subList(Math.max(0, previos.size() - MAX_HISTORIAL), previos.size()));
        conversacion.add(new MensajeIA(MensajeIA.Rol.USUARIO, mensajeUsuario));
        return conversacion;
    }
}
