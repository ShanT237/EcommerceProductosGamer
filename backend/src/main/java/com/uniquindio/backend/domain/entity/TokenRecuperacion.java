package com.uniquindio.backend.domain.entity;

import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Rol;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad TokenRecuperacion: permite a una cuenta (comprador o vendedor) elegir una contraseña
 * nueva sin conocer la actual. Vence a los pocos minutos y se elimina al usarse.
 * Solo guarda el hash del token, nunca el token en claro.
 */
@Getter
public class TokenRecuperacion {

    private final UUID id;
    private final String hashToken;
    private final UUID cuentaId;
    private final Rol rol;
    private final Instant creadoEn;
    private final Instant expiraEn;

    private TokenRecuperacion(UUID id, String hashToken, UUID cuentaId, Rol rol,
                              Instant creadoEn, Instant expiraEn) {
        this.id = id;
        this.hashToken = hashToken;
        this.cuentaId = cuentaId;
        this.rol = rol;
        this.creadoEn = creadoEn;
        this.expiraEn = expiraEn;
    }

    public static TokenRecuperacion emitir(UUID id, String hashToken, UUID cuentaId, Rol rol,
                                           Instant ahora, Duration vigencia) {
        Objects.requireNonNull(id, "El id del token es obligatorio");
        Objects.requireNonNull(cuentaId, "La cuenta del token es obligatoria");
        Objects.requireNonNull(rol, "El rol de la cuenta es obligatorio");
        Objects.requireNonNull(ahora, "La fecha de emisión es obligatoria");
        if (hashToken == null || hashToken.isBlank()) {
            throw new ReglaDominioException("El token de recuperación no puede estar vacío");
        }
        if (vigencia == null || vigencia.isZero() || vigencia.isNegative()) {
            throw new ReglaDominioException("La vigencia del token debe ser positiva");
        }
        return new TokenRecuperacion(id, hashToken, cuentaId, rol, ahora, ahora.plus(vigencia));
    }

    /** Solo para el adaptador de persistencia: revive un token que YA existía. */
    public static TokenRecuperacion reconstruir(UUID id, String hashToken, UUID cuentaId, Rol rol,
                                                Instant creadoEn, Instant expiraEn) {
        return new TokenRecuperacion(id, hashToken, cuentaId, rol, creadoEn, expiraEn);
    }

    public boolean estaVigente(Instant ahora) {
        return ahora.isBefore(expiraEn);
    }

    public boolean emitidoHaceMenosDe(Duration lapso, Instant ahora) {
        return creadoEn.plus(lapso).isAfter(ahora);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TokenRecuperacion otro)) return false;
        return id.equals(otro.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
