package com.uniquindio.backend.infrastructure.config;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.VerificadorCuentaActiva;
import com.uniquindio.backend.infrastructure.security.JwtAuthenticationFilter;
import com.uniquindio.backend.infrastructure.security.RespuestaErrorSeguridad;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * Seguridad stateless con JWT: login, registros y la consulta del catálogo (productos, combos y
 * preventas, solo GET) son públicos; el resto de /api/** exige token.
 * Los 401 (sin token, token inválido o cuenta dada de baja) y 403 (sin permiso) responden JSON.
 * El filtro JWT consulta en cada petición que la cuenta siga activa, así que una baja corta
 * la sesión al instante en lugar de esperar a que expire el token.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Código estable en el cuerpo del 401 cuando el token es válido pero la cuenta fue dada de baja. */
    public static final String CODIGO_CUENTA_INACTIVA = "CUENTA_INACTIVA";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, EmisorToken emisorToken,
                                                   VerificadorCuentaActiva verificadorCuentaActiva) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2
                .authorizeHttpRequests(auth -> auth
                        // El re-despacho asíncrono (al completar/expirar un SseEmitter) no vuelve a pasar por
                        // JwtAuthenticationFilter (OncePerRequestFilter); sin esto se rechazaría con la respuesta
                        // ya enviada. El despacho REQUEST inicial sigue exigiendo autenticación.
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/vendedores").permitAll()
                        // Catálogo público: un visitante puede ver productos y combos, pero no comprarlos.
                        .requestMatchers(HttpMethod.GET, "/api/productos", "/api/combos", "/api/preventas").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> {
                            if (request.getAttribute(JwtAuthenticationFilter.ATRIBUTO_CUENTA_INACTIVA) != null) {
                                RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED,
                                        "La cuenta se encuentra inactiva o fue dada de baja", CODIGO_CUENTA_INACTIVA);
                            } else {
                                RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED,
                                        "Token ausente, inválido o expirado");
                            }
                        })
                        .accessDeniedHandler((request, response, ex) ->
                                RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN,
                                        "No tienes permisos para realizar esta operación")))
                .addFilterBefore(new JwtAuthenticationFilter(emisorToken, verificadorCuentaActiva),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * La autenticación la hace el filtro JWT; este bean evita que Spring Boot cree un
     * usuario en memoria con contraseña generada.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("Autenticación solo por JWT");
        };
    }

    /**
     * CORS para el frontend (Angular/React en desarrollo). Debe vivir en la cadena de seguridad:
     * el preflight OPTIONS se responde antes de exigir token. Swagger se sirve desde el mismo
     * origen que la API, así que no necesita CORS.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins}") String[] origenesPermitidos) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(origenesPermitidos));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
