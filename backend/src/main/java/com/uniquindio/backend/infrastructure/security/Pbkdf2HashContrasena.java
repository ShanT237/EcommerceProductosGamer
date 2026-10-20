package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.HashContrasena;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2-HMAC-SHA256 con sal aleatoria (algoritmo del JDK, sin dependencias externas).
 * Formato almacenado: base64(sal):base64(hash).
 */
@Component
public class Pbkdf2HashContrasena implements HashContrasena {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 120_000;
    private static final int BITS_CLAVE = 256;
    private static final int BYTES_SAL = 16;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String hashear(String plana) {
        byte[] sal = new byte[BYTES_SAL];
        random.nextBytes(sal);
        Base64.Encoder b64 = Base64.getEncoder();
        return b64.encodeToString(sal) + ":" + b64.encodeToString(derivar(plana, sal));
    }

    @Override
    public boolean coincide(String plana, String hashAlmacenado) {
        if (plana == null || hashAlmacenado == null) {
            return false;
        }
        String[] partes = hashAlmacenado.split(":", -1);
        if (partes.length != 2) {
            return false;
        }
        try {
            byte[] sal = Base64.getDecoder().decode(partes[0]);
            byte[] esperado = Base64.getDecoder().decode(partes[1]);
            return MessageDigest.isEqual(esperado, derivar(plana, sal));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private byte[] derivar(String plana, byte[] sal) {
        try {
            PBEKeySpec spec = new PBEKeySpec(plana.toCharArray(), sal, ITERACIONES, BITS_CLAVE);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        }
    }
}
