package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.SesionToken;
import com.uniquindio.backend.domain.valueobject.Rol;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtEmisorTokenTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";
    private static final Instant AHORA = Instant.parse("2026-10-01T10:00:00Z");

    private JwtEmisorToken emisor(String secreto, Instant instante) {
        return new JwtEmisorToken(secreto, 2, Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void emiteUnTokenQueSeValidaYDevuelveUsuarioYRol() {
        UUID id = UUID.randomUUID();
        String token = emisor(SECRETO, AHORA).emitir(id, "gamer@test.com", Rol.VENDEDOR);

        assertEquals(3, token.split("\\.").length);
        SesionToken sesion = emisor(SECRETO, AHORA.plusSeconds(60)).validar(token).orElseThrow();
        assertEquals(id, sesion.usuarioId());
        assertEquals(Rol.VENDEDOR, sesion.rol());
    }

    @Test
    void rechazaTokenExpirado() {
        String token = emisor(SECRETO, AHORA).emitir(UUID.randomUUID(), "gamer@test.com", Rol.COMPRADOR);

        assertTrue(emisor(SECRETO, AHORA.plus(Duration.ofHours(2)).plusSeconds(1)).validar(token).isEmpty());
        assertTrue(emisor(SECRETO, AHORA.plus(Duration.ofHours(2)).minusSeconds(1)).validar(token).isPresent());
    }

    @Test
    void rechazaTokenFirmadoConOtroSecretoOAlterado() {
        UUID id = UUID.randomUUID();
        String token = emisor(SECRETO, AHORA).emitir(id, "gamer@test.com", Rol.COMPRADOR);

        assertTrue(emisor("otro-secreto-distinto-con-mas-de-32-caracteres", AHORA).validar(token).isEmpty());

        String[] partes = token.split("\\.");
        String otroPayload = emisor(SECRETO, AHORA)
                .emitir(UUID.randomUUID(), "otro@test.com", Rol.VENDEDOR).split("\\.")[1];
        assertTrue(emisor(SECRETO, AHORA).validar(partes[0] + "." + otroPayload + "." + partes[2]).isEmpty());
    }

    @Test
    void rechazaTokenSinClaimDeRolOConRolDesconocido() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
        Date expira = Date.from(AHORA.plusSeconds(3600));

        String sinRol = Jwts.builder().subject(UUID.randomUUID().toString())
                .expiration(expira).signWith(clave).compact();
        String rolDesconocido = Jwts.builder().subject(UUID.randomUUID().toString())
                .claim("rol", "SUPERADMIN").expiration(expira).signWith(clave).compact();

        assertTrue(emisor(SECRETO, AHORA).validar(sinRol).isEmpty());
        assertTrue(emisor(SECRETO, AHORA).validar(rolDesconocido).isEmpty());
    }

    @Test
    void rechazaTokensMalFormados() {
        JwtEmisorToken emisor = emisor(SECRETO, AHORA);

        assertTrue(emisor.validar(null).isEmpty());
        assertTrue(emisor.validar("").isEmpty());
        assertTrue(emisor.validar("a.b").isEmpty());
        assertTrue(emisor.validar("a.b.c").isEmpty());
    }

    @Test
    void rechazaSecretoCorto() {
        assertThrows(IllegalArgumentException.class, () -> emisor("corto", AHORA));
    }
}