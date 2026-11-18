package com.uniquindio.backend.application.port.out;

/**
 * Puerto: cómo se envía un correo. El caso de uso no sabe qué proveedor lo manda.
 * El envío es "de mejor esfuerzo": no lanza excepciones al llamador y no bloquea la respuesta HTTP.
 */
public interface EnviadorCorreos {
    void enviar(String destinatario, String asunto, String contenidoHtml);
}
