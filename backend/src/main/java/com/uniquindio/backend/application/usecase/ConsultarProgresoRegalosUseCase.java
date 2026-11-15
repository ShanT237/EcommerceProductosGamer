package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProgresoRegalosResponse;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Caso de uso: ConsultarProgresoRegalos.
 * Usa la misma regla que AsignarRegaloPorMeta (un regalo por cada COMPRAS_POR_REGALO compras
 * completadas) para mostrar cuántos regalos tiene el comprador por elegir y cuánto le falta.
 */
@Service
@RequiredArgsConstructor
public class ConsultarProgresoRegalosUseCase {

    private final CompraRepository compraRepository;
    private final RegaloRepository regaloRepository;

    @Transactional(readOnly = true)
    public ProgresoRegalosResponse ejecutar(UUID usuarioId) {
        int comprasPorRegalo = Usuario.COMPRAS_POR_REGALO;
        long completadas = compraRepository.contarCompletadasDe(usuarioId);
        long asignados = regaloRepository.contarAsignadosA(usuarioId);

        long disponibles = Math.max(0, completadas / comprasPorRegalo - asignados);
        int paraSiguiente = comprasPorRegalo - (int) (completadas % comprasPorRegalo);

        return new ProgresoRegalosResponse(completadas, comprasPorRegalo, asignados, disponibles, paraSiguiente);
    }
}
