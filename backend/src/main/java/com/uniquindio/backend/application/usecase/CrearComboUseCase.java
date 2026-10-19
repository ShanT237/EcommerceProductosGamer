package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CrearCombo (CU-07).
 * Valida vendedor activo y que cada producto exista, no esté eliminado y sea del vendedor;
 * las reglas del combo (mínimo 2 productos, sin duplicados) las aplica la entidad.
 */
@Service
@RequiredArgsConstructor
public class CrearComboUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;

    public Combo ejecutar(UUID id, UUID vendedorId, String nombre, String descripcion,
                          double descuento, List<UUID> productoIds) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.validarPuedePublicar();

        if (productoIds != null) {
            productoIds.forEach(productoId -> validarProductoDelVendedor(productoId, vendedorId));
        }

        Combo combo = Combo.crear(id, vendedorId, nombre, descripcion, descuento, productoIds);

        comboRepository.guardar(combo);

        return combo;
    }

    private void validarProductoDelVendedor(UUID productoId, UUID vendedorId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();
        if (!producto.getVendedorId().equals(vendedorId)) {
            throw new ReglaDominioException("El producto no pertenece al vendedor del combo");
        }
    }
}
