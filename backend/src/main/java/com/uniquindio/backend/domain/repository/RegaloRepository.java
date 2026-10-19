package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.Regalo;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository de Regalo.
 * Se crea UNA sola vez y lo reutilizan todos los casos de uso que necesiten
 * consultar o guardar regalos.
 */
public interface RegaloRepository {

    Optional<Regalo> obtenerPorId(UUID id);

    void guardar(Regalo regalo);
}