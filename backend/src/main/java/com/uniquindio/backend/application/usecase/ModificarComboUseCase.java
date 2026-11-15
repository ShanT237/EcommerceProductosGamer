package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/** Caso de uso: ModificarCombo. El vendedor dueño cambia nombre, descripción y descuento. */
@Service
@RequiredArgsConstructor
public class ModificarComboUseCase {

    private final ComboRepository comboRepository;

    public Combo ejecutar(UUID comboId, UUID vendedorId, String nombre, String descripcion, double descuento) {
        Combo combo = comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new NoSuchElementException("El combo no existe"));
        combo.asegurarPerteneceA(vendedorId);

        combo.modificar(nombre, descripcion, descuento);

        comboRepository.guardar(combo);
        return combo;
    }
}
