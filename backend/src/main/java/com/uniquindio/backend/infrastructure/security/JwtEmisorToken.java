package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.EmisorToken;
import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.valueobject.Rol;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT HS256 con jjwt. Claims: sub (id del usuario), email, rol, iat y exp.
 * El reloj es inyectado para poder probar la expiración.
 */
@Component
public class JwtEmisorToken implements EmisorToken {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_ROL = "rol";
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final SecretKey clave;
    private final long horasExpiracion;
    private final Clock clock;

    public JwtEmisorToken(@Value("${jwt.secret}") String secreto,
                          @Value("${jwt.horas-expiracion:2}") long horasExpiracion,
                          Clock clock) {
        if (secreto == null || secreto.length() < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalArgumentException(
                    "El secreto JWT debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.horasExpiracion = horasExpiracion;
        this.clock = clock;
    }

    @Override
    public String emitir(UUID usuarioId, String correo, Rol rol) {
        Instant ahora = clock.instant();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim(CLAIM_EMAIL, correo)
                .claim(CLAIM_ROL, rol.name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofHours(horasExpiracion))))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Optional<SesionToken> validar(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String rol = claims.get(CLAIM_ROL, String.class);
            if (claims.getSubject() == null || rol == null) {
                return Optional.empty();
            }
            return Optional.of(new SesionToken(UUID.fromString(claims.getSubject()), Rol.valueOf(rol)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}