package com.uniquindio.backend.infrastructure.security;

import com.uniquindio.backend.domain.service.EmisorToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JWT HS256 mínimo con javax.crypto (sin librerías externas).
 * Claims: sub (id del usuario), email, iat y exp en segundos epoch.
 */
@Component
public class JwtEmisorToken implements EmisorToken {

    private static final String HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
    private static final Pattern EXP = Pattern.compile("\"exp\":(\\d+)");
    private static final Pattern SUB = Pattern.compile("\"sub\":\"([0-9a-fA-F-]{36})\"");
    private static final int LONGITUD_MINIMA_SECRETO = 32;

    private final byte[] secreto;
    private final long horasExpiracion;
    private final Clock clock;

    public JwtEmisorToken(@Value("${jwt.secret}") String secreto,
                          @Value("${jwt.horas-expiracion:2}") long horasExpiracion,
                          Clock clock) {
        if (secreto == null || secreto.length() < LONGITUD_MINIMA_SECRETO) {
            throw new IllegalArgumentException(
                    "El secreto JWT debe tener al menos " + LONGITUD_MINIMA_SECRETO + " caracteres");
        }
        this.secreto = secreto.getBytes(StandardCharsets.UTF_8);
        this.horasExpiracion = horasExpiracion;
        this.clock = clock;
    }

    @Override
    public String emitir(UUID usuarioId, String correo) {
        long ahora = clock.instant().getEpochSecond();
        long expira = ahora + horasExpiracion * 3600;
        String payload = "{\"sub\":\"" + usuarioId + "\",\"email\":\"" + escapar(correo)
                + "\",\"iat\":" + ahora + ",\"exp\":" + expira + "}";
        String cuerpo = codificar(HEADER) + "." + codificar(payload);
        return cuerpo + "." + firmar(cuerpo);
    }

    @Override
    public Optional<UUID> validar(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] partes = token.split("\\.", -1);
        if (partes.length != 3) {
            return Optional.empty();
        }
        byte[] firmaRecibida = partes[2].getBytes(StandardCharsets.UTF_8);
        byte[] firmaEsperada = firmar(partes[0] + "." + partes[1]).getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(firmaEsperada, firmaRecibida)) {
            return Optional.empty();
        }
        try {
            String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
            Matcher exp = EXP.matcher(payload);
            Matcher sub = SUB.matcher(payload);
            if (!exp.find() || !sub.find()
                    || clock.instant().getEpochSecond() >= Long.parseLong(exp.group(1))) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(sub.group(1)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private String firmar(String contenido) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(contenido.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar el token", e);
        }
    }

    private static String codificar(String texto) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapar(String texto) {
        return texto == null ? "" : texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
