package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ModificarProductosCombo (CU-23).
 * El vendedor dueño del combo agrega o quita productos; el mínimo de 2 lo garantiza la entidad.
 */
@Service
@RequiredArgsConstructor
public class ModificarProductosComboUseCase {

    private final ComboRepository comboRepository;
    private final ProductoRepository productoRepository;

    public Combo agregar(UUID comboId, UUID vendedorId, UUID productoId) {
        Combo combo = obtenerCombo(comboId);
        combo.asegurarPerteneceA(vendedorId);

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();
        if (!producto.getVendedorId().equals(combo.getVendedorId())) {
            throw new ReglaDominioException("El producto no pertenece al vendedor del combo");
        }

        combo.agregarProducto(productoId);

        comboRepository.guardar(combo);

        return combo;
    }

    public Combo quitar(UUID comboId, UUID vendedorId, UUID productoId) {
        Combo combo = obtenerCombo(comboId);
        combo.asegurarPerteneceA(vendedorId);

        combo.quitarProducto(productoId);

        comboRepository.guardar(combo);

        return combo;
    }

    private Combo obtenerCombo(UUID comboId) {
        return comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new NoSuchElementException("El combo no existe"));
    }
}