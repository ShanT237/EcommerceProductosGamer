package com.uniquindio.backend.application.port.out;

import java.util.List;

/**
 * Puerto: cómo se le pide una respuesta a un modelo de IA. El caso de uso no sabe qué proveedor
 * (ni qué modelo) contesta.
 * Devuelve el texto de la respuesta; si el proveedor no está configurado, no responde o devuelve
 * algo inválido, lanza una {@code ResponseStatusException} con un mensaje apto para el usuario.
 */
public interface AsistenteIAPort {

    String responder(List<MensajeIA> conversacion);
}
