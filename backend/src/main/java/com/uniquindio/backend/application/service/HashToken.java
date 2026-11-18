package com.uniquindio.backend.application.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Genera y protege los tokens de recuperación de contraseña. El token en claro solo viaja en el
 * correo; en la base de datos se guarda únicamente su hash SHA-256, así que un volcado de la base
 * no sirve para restablecer cuentas.
 */
public final class HashToken {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashToken() {
    }

    /** Token aleatorio de 256 bits, seguro para ir en una URL. */
    public static String generar() {
        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String hashear(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("El algoritmo SHA-256 no está disponible", e);
        }
    }
}
