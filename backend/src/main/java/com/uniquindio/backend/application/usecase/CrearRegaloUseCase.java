package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: CrearRegalo.
 * El vendedor activo ofrece un regalo (queda DISPONIBLE) sobre un producto o un combo PROPIO.
 * Que sea exactamente uno de los dos lo valida Regalo.crear.
 */
@Service
@RequiredArgsConstructor
public class CrearRegaloUseCase {

    private final VendedorRepository vendedorRepository;
    private final ProductoRepository productoRepository;
    private final ComboRepository comboRepository;
    private final RegaloRepository regaloRepository;

    public Regalo ejecutar(UUID id, UUID vendedorId, UUID productoId, UUID comboId) {
        Vendedor vendedor = vendedorRepository.obtenerPorId(vendedorId)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"));
        vendedor.validarPuedePublicar();

        Regalo regalo = Regalo.crear(id, productoId, comboId);

        if (productoId != null) {
            validarProductoDelVendedor(productoId, vendedorId);
        } else {
            validarComboDelVendedor(comboId, vendedorId);
        }

        regaloRepository.guardar(regalo);
        return regalo;
    }

    private void validarProductoDelVendedor(UUID productoId, UUID vendedorId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();
        producto.asegurarPerteneceA(vendedorId);
    }

    private void validarComboDelVendedor(UUID comboId, UUID vendedorId) {
        Combo combo = comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new ReglaDominioException("El combo no existe"));
        combo.asegurarPerteneceA(vendedorId);
    }
}
