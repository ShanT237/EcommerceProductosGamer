package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: EliminarCombo.
 * Solo el vendedor dueño. No se elimina si hay un regalo sin cerrar (disponible, elegido o enviado)
 * que lo referencie, porque Regalo guarda el id del combo sin clave foránea.
 */
@Service
@RequiredArgsConstructor
public class EliminarComboUseCase {

    private final ComboRepository comboRepository;
    private final RegaloRepository regaloRepository;

    @Transactional
    public void ejecutar(UUID comboId, UUID vendedorId) {
        Combo combo = comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new NoSuchElementException("El combo no existe"));
        combo.asegurarPerteneceA(vendedorId);

        if (regaloRepository.existePendienteDelCombo(comboId)) {
            throw new ReglaDominioException(
                    "No se puede eliminar el combo: tiene regalos pendientes de entrega");
        }

        comboRepository.eliminar(comboId);
    }
}
