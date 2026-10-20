package com.uniquindio.backend.domain.repository;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;

import java.util.List;
import java.util.UUID;

public interface MovimientoPuntosRepository {
    void guardar(MovimientoPuntos movimiento);
    List<MovimientoPuntos> obtenerPorUsuarioId(UUID usuarioId);
}
