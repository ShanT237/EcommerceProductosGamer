package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.domain.service.EmisorToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;
import java.util.UUID;

/**
 * Exige un token JWT válido ("Authorization: Bearer ...") en las rutas protegidas.
 * Si es válido deja el id del usuario en el atributo "usuarioId" de la petición.
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    public static final String ATRIBUTO_USUARIO_ID = "usuarioId";
    private static final String PREFIJO = "Bearer ";

    private final EmisorToken emisorToken;

    public JwtAuthInterceptor(EmisorToken emisorToken) {
        this.emisorToken = emisorToken;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (esRegistroPublico(request)) {
            return true;
        }
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            Optional<UUID> usuarioId = emisorToken.validar(cabecera.substring(PREFIJO.length()).trim());
            if (usuarioId.isPresent()) {
                request.setAttribute(ATRIBUTO_USUARIO_ID, usuarioId.get());
                return true;
            }
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    private boolean esRegistroPublico(HttpServletRequest request) {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String ruta = request.getServletPath();
        return "/api/usuarios".equals(ruta) || "/api/vendedores".equals(ruta);
    }
}
