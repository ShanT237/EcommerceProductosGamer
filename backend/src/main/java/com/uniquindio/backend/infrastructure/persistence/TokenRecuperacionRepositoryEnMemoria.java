package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.TokenRecuperacion;
import com.uniquindio.backend.domain.repository.TokenRecuperacionRepository;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class TokenRecuperacionRepositoryEnMemoria implements TokenRecuperacionRepository {

    private final Map<UUID, TokenRecuperacion> tokens = new HashMap<>();

    @Override
    public void guardar(TokenRecuperacion token) {
        tokens.put(token.getId(), token);
    }

    @Override
    public Optional<TokenRecuperacion> obtenerPorHash(String hashToken) {
        return tokens.values().stream()
                .filter(t -> t.getHashToken().equals(hashToken))
                .findFirst();
    }

    @Override
    public Optional<TokenRecuperacion> obtenerUltimoDeCuenta(UUID cuentaId) {
        return tokens.values().stream()
                .filter(t -> t.getCuentaId().equals(cuentaId))
                .max(Comparator.comparing(TokenRecuperacion::getCreadoEn));
    }

    @Override
    public void eliminarDeCuenta(UUID cuentaId) {
        tokens.values().removeIf(t -> t.getCuentaId().equals(cuentaId));
    }

    public int cantidad() {
        return tokens.size();
    }
}
