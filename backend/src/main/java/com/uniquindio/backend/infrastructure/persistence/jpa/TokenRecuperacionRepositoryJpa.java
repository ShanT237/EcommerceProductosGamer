package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.TokenRecuperacion;
import com.uniquindio.backend.domain.repository.TokenRecuperacionRepository;
import com.uniquindio.backend.domain.valueobject.Rol;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.TokenRecuperacionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TokenRecuperacionRepositoryJpa implements TokenRecuperacionRepository {

    private final TokenRecuperacionJpaDataRepository dataRepository;

    @Override
    public void guardar(TokenRecuperacion token) {
        dataRepository.save(aEntidad(token));
    }

    @Override
    public Optional<TokenRecuperacion> obtenerPorHash(String hashToken) {
        return dataRepository.findByHashToken(hashToken).map(this::aDominio);
    }

    @Override
    public Optional<TokenRecuperacion> obtenerUltimoDeCuenta(UUID cuentaId) {
        return dataRepository.findFirstByCuentaIdOrderByCreadoEnDesc(cuentaId).map(this::aDominio);
    }

    @Override
    @Transactional
    public void eliminarDeCuenta(UUID cuentaId) {
        dataRepository.deleteByCuentaId(cuentaId);
    }

    private TokenRecuperacionEntity aEntidad(TokenRecuperacion token) {
        TokenRecuperacionEntity entidad = new TokenRecuperacionEntity();
        entidad.setId(token.getId());
        entidad.setHashToken(token.getHashToken());
        entidad.setCuentaId(token.getCuentaId());
        entidad.setRol(token.getRol().name());
        entidad.setCreadoEn(token.getCreadoEn());
        entidad.setExpiraEn(token.getExpiraEn());
        return entidad;
    }

    private TokenRecuperacion aDominio(TokenRecuperacionEntity entidad) {
        return TokenRecuperacion.reconstruir(entidad.getId(), entidad.getHashToken(),
                entidad.getCuentaId(), Rol.valueOf(entidad.getRol()),
                entidad.getCreadoEn(), entidad.getExpiraEn());
    }
}
