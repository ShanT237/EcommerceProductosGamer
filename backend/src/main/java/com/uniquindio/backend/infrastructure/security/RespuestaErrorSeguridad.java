package com.uniquindio.backend.infrastructure.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Cuerpo JSON de los errores 401 y 403, con el mismo formato que GlobalExceptionHandler.
 * Los errores de seguridad ocurren en el filtro, antes de llegar a @RestControllerAdvice.
 */
public final class RespuestaErrorSeguridad {

    private RespuestaErrorSeguridad() {
    }

    public static void escribir(HttpServletResponse response, HttpStatus status, String mensaje) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"timestamp\":\"" + LocalDateTime.now()
                + "\",\"status\":" + status.value()
                + ",\"mensaje\":\"" + mensaje + "\"}");
    }
}