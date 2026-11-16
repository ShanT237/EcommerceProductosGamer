package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.domain.repository.MetaRecompensaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Caso de uso: ConsultarMetas (CU-33, lectura).
 * Devuelve solo las metas activas, de menor a mayor umbral de compras. Las metas se muestran
 * como información: no gobiernan la asignación de regalos (eso sigue siendo
 * Usuario.COMPRAS_POR_REGALO).
 */
@Service
@RequiredArgsConstructor
public class ConsultarMetasUseCase {

    private final MetaRecompensaRepository metaRepository;

    @Transactional(readOnly = true)
    public List<MetaRecompensa> ejecutar() {
        return metaRepository.obtenerActivas().stream()
                .sorted(Comparator.comparingInt(MetaRecompensa::getComprasRequeridas))
                .toList();
    }
}
