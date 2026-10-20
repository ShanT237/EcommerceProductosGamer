package com.uniquindio.backend.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtEmisorTokenTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";
    private static final Instant AHORA = Instant.parse("2026-10-01T10:00:00Z");

    private JwtEmisorToken emisor(String secreto, Instant instante) {
        return new JwtEmisorToken(secreto, 2, Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void emiteUnTokenQueSeValidaYDevuelveElUsuario() {
        UUID id = UUID.randomUUID();
        String token = emisor(SECRETO, AHORA).emitir(id, "gamer@test.com");

        assertEquals(3, token.split("\\.").length);
        assertEquals(id, emisor(SECRETO, AHORA.plusSeconds(60)).validar(token).orElseThrow());
    }

    @Test
    void rechazaTokenExpirado() {
        String token = emisor(SECRETO, AHORA).emitir(UUID.randomUUID(), "gamer@test.com");

        assertTrue(emisor(SECRETO, AHORA.plus(Duration.ofHours(2))).validar(token).isEmpty());
        assertTrue(emisor(SECRETO, AHORA.plus(Duration.ofHours(2)).minusSeconds(1)).validar(token).isPresent());
    }

    @Test
    void rechazaTokenFirmadoConOtroSecretoOAlterado() {
        UUID id = UUID.randomUUID();
        String token = emisor(SECRETO, AHORA).emitir(id, "gamer@test.com");

        assertTrue(emisor("otro-secreto-distinto-con-mas-de-32-caracteres", AHORA).validar(token).isEmpty());

        String[] partes = token.split("\\.");
        String otroPayload = emisor(SECRETO, AHORA).emitir(UUID.randomUUID(), "otro@test.com").split("\\.")[1];
        assertTrue(emisor(SECRETO, AHORA).validar(partes[0] + "." + otroPayload + "." + partes[2]).isEmpty());
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
