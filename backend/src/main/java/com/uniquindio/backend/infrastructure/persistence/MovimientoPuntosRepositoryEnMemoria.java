package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.repository.MovimientoPuntosRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
public class MovimientoPuntosRepositoryEnMemoria implements MovimientoPuntosRepository {

    private final List<MovimientoPuntos> movimientos = new ArrayList<>();

    @Override
    public void guardar(MovimientoPuntos movimiento) {
        movimientos.add(movimiento);
    }

    @Override
    public List<MovimientoPuntos> obtenerPorUsuarioId(UUID usuarioId) {
        return movimientos.stream()
                .filter(m -> m.getUsuarioId().equals(usuarioId))
                .toList();
    }
}
