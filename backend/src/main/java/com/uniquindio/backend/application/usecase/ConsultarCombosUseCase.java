package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: ConsultarCombos.
 *  - todos(): catálogo para compradores. Solo combos cuyos productos existen y están TODOS visibles
 *    (ni eliminados ni pausados), porque si uno no se puede comprar, el combo tampoco.
 *  - deVendedor(): todos los combos del vendedor, aunque tengan productos pausados o eliminados,
 *    para que pueda gestionarlos.
 */
@Service
@RequiredArgsConstructor
public class ConsultarCombosUseCase {

    private final ComboRepository comboRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ComboConProductos> todos() {
        List<ComboConProductos> resultado = new ArrayList<>();
        for (Combo combo : comboRepository.obtenerTodos()) {
            ComboConProductos conProductos = resolver(combo);
            boolean completo = conProductos.productos().size() == combo.getProductos().size();
            if (completo && conProductos.productos().stream().allMatch(Producto::estaVisible)) {
                resultado.add(conProductos);
            }
        }
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<ComboConProductos> deVendedor(UUID vendedorId) {
        return comboRepository.obtenerPorVendedorId(vendedorId).stream()
                .map(this::resolver)
                .toList();
    }

    /** Resuelve los productos del combo; los ids que ya no existen se omiten. */
    private ComboConProductos resolver(Combo combo) {
        List<Producto> productos = combo.getProductos().stream()
                .map(productoRepository::obtenerPorId)
                .flatMap(Optional::stream)
                .toList();
        return new ComboConProductos(combo, productos);
    }
}
