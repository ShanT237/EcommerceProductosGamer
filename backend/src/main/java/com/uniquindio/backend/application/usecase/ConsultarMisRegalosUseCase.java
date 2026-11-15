package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarMisRegalos.
 * Los regalos que el comprador eligió (elegidos, enviados, entregados o cancelados).
 */
@Service
@RequiredArgsConstructor
public class ConsultarMisRegalosUseCase {

    private final RegaloRepository regaloRepository;

    @Transactional(readOnly = true)
    public List<Regalo> ejecutar(UUID usuarioId) {
        return regaloRepository.obtenerPorUsuarioId(usuarioId);
    }
}
