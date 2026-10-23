package com.uniquindio.backend.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BCryptHashContrasenaTest {

    private final BCryptHashContrasena hasher = new BCryptHashContrasena();

    @Test
    void elHashNuncaEsLaContrasenaEnClaro() {
        String hash = hasher.hashear("Gamer2026");

        assertNotEquals("Gamer2026", hash);
        assertFalse(hash.contains("Gamer2026"));
        assertTrue(hash.startsWith("$2"));
    }

    @Test
    void coincideSoloConLaContrasenaCorrecta() {
        String hash = hasher.hashear("Gamer2026");

        assertTrue(hasher.coincide("Gamer2026", hash));
        assertFalse(hasher.coincide("gamer2026", hash));
        assertFalse(hasher.coincide(null, hash));
        assertFalse(hasher.coincide("Gamer2026", null));
        assertFalse(hasher.coincide("Gamer2026", "basura-sin-formato"));
    }

    @Test
    void laMismaContrasenaGeneraHashesDistintosPorLaSal() {
        assertNotEquals(hasher.hashear("Gamer2026"), hasher.hashear("Gamer2026"));
    }
}