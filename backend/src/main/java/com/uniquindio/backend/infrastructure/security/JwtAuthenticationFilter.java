package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
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
 * Además de la firma y la expiración, comprueba en cada petición que la cuenta siga activa:
 * tras una baja, el token ya emitido deja de servir de inmediato. En ese caso no autentica y
 * marca la petición con ATRIBUTO_CUENTA_INACTIVA para que el entry point explique el motivo.
 * No es un @Component a propósito: lo registra SecurityConfig dentro de la cadena
 * (si fuera bean, Spring Boot lo registraría también como filtro de servlet).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO = "Bearer ";

    /** Atributo de la petición que indica que el token era válido pero la cuenta está inactiva. */
    public static final String ATRIBUTO_CUENTA_INACTIVA = "cuentaInactiva";

    private final EmisorToken emisorToken;
    private final VerificadorCuentaActiva verificadorCuentaActiva;

    public JwtAuthenticationFilter(EmisorToken emisorToken, VerificadorCuentaActiva verificadorCuentaActiva) {
        this.emisorToken = emisorToken;
        this.verificadorCuentaActiva = verificadorCuentaActiva;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String cabecera = request.getHeader("Authorization");
        if (cabecera != null && cabecera.startsWith(PREFIJO)) {
            emisorToken.validar(cabecera.substring(PREFIJO.length()).trim()).ifPresent(sesion -> {
                if (!verificadorCuentaActiva.estaActiva(sesion)) {
                    request.setAttribute(ATRIBUTO_CUENTA_INACTIVA, true);
                    return;
                }
                var autenticacion = new UsernamePasswordAuthenticationToken(
                        sesion.usuarioId(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + sesion.rol().name())));
                SecurityContextHolder.getContext().setAuthentication(autenticacion);
            });
        }
        chain.doFilter(request, response);
    }
}