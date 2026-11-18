package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.TokenRecuperacion;

import java.util.Optional;
import java.util.UUID;

public interface TokenRecuperacionRepository {

    void guardar(TokenRecuperacion token);

    Optional<TokenRecuperacion> obtenerPorHash(String hashToken);

    /** El token más reciente de la cuenta; sirve para no enviar correos en ráfaga. */
    Optional<TokenRecuperacion> obtenerUltimoDeCuenta(UUID cuentaId);

    /** Elimina todos los tokens de la cuenta (al emitir uno nuevo o al usarlo). */
    void eliminarDeCuenta(UUID cuentaId);
}
