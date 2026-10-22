package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.repository.MovimientoPuntosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarHistorialPuntos (CU-32).
 * Puntos ganados y redimidos del usuario, el más reciente primero.
 */
@Service
@RequiredArgsConstructor
public class ConsultarHistorialPuntosUseCase {

    private final MovimientoPuntosRepository movimientoRepository;

    @Transactional(readOnly = true)
    public List<MovimientoPuntos> ejecutar(UUID usuarioId) {
        return movimientoRepository.obtenerPorUsuarioId(usuarioId).stream()
                .sorted(Comparator.comparing(MovimientoPuntos::getFecha).reversed())
                .toList();
    }
}
