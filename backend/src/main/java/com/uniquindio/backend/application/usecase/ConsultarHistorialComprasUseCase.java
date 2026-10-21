package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.repository.CompraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarHistorialCompras (CU-31).
 * Compras del usuario, la más reciente primero.
 */
@Service
@RequiredArgsConstructor
public class ConsultarHistorialComprasUseCase {

    private final CompraRepository compraRepository;

    @Transactional(readOnly = true)
    public List<Compra> ejecutar(UUID usuarioId) {
        return compraRepository.obtenerPorUsuarioId(usuarioId).stream()
                .sorted(Comparator.comparing(Compra::getFecha).reversed())
                .toList();
    }
}
