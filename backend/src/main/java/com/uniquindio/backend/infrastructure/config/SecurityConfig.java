package com.uniquindio.backend.infrastructure.config;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.infrastructure.security.JwtAuthenticationFilter;
import com.uniquindio.backend.infrastructure.security.RespuestaErrorSeguridad;
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

/**
 * Seguridad stateless con JWT: login y registros son públicos, el resto de /api/** exige token.
 * Los 401 (sin token o token inválido) y 403 (sin permiso) responden JSON.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, EmisorToken emisorToken) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/usuarios", "/api/vendedores").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/api-docs/**", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) ->
                                RespuestaErrorSeguridad.escribir(response, HttpStatus.UNAUTHORIZED,
                                        "Token ausente, inválido o expirado"))
                        .accessDeniedHandler((request, response, ex) ->
                                RespuestaErrorSeguridad.escribir(response, HttpStatus.FORBIDDEN,
                                        "No tienes permisos para realizar esta operación")))
                .addFilterBefore(new JwtAuthenticationFilter(emisorToken),
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
}