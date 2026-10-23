package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.EmisorToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Lee "Authorization: Bearer ...", valida el JWT y deja la autenticación en el contexto:
 * el principal es el UUID del usuario y la autoridad es ROLE_<rol>.
 * Si el token falta o es inválido no autentica; el 401 lo responde el entry point.
 * No es un @Component a propósito: lo registra SecurityConfig dentro de la cadena
 * (si fuera bean, Spring Boot lo registraría también como filtro de servlet).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    private final EmisorToken emisorToken;

    public JwtAuthenticationFilter(EmisorToken emisorToken) {
        this.emisorToken = emisorToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            emisorToken.validar(cabecera.substring(PREFIJO.length()).trim()).ifPresent(sesion -> {
                var autenticacion = new UsernamePasswordAuthenticationToken(
                        sesion.usuarioId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + sesion.rol().name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            });
        }
        chain.doFilter(request, response);
    }
}