package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.*;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ComprarCombo (CU-16).
 * Genera una compra PENDIENTE por cada producto del combo, con el descuento del combo
 * aplicado al precio base. Todo se valida ANTES de descontar stock: si un solo producto
 * falla, no se modifica ninguno ni se guarda ninguna compra.
 */
@Service
@RequiredArgsConstructor
public class ComprarComboUseCase {

    private final ComboRepository comboRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    public List<Compra> ejecutar(UUID usuarioId, UUID comboId, int cantidad) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        Combo combo = comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new NoSuchElementException("El combo no existe"));

        List<Producto> productos = new ArrayList<>();
        for (UUID productoId : combo.getProductos()) {
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
            validarProducto(producto, usuarioId, cantidad, ahora);
            productos.add(producto);
        }

        List<Compra> compras = new ArrayList<>();
        for (Producto producto : productos) {
            producto.reducirStock(cantidad);
            Precio precioUnitario = new Precio(producto.getPrecio()).aplicarDescuento(combo.getDescuento());
            compras.add(Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), cantidad,
                    ahora, precioUnitario));
        }

        productos.forEach(productoRepository::guardar);
        compras.forEach(compraRepository::guardar);
        return compras;
    }

    private void validarProducto(Producto producto, UUID usuarioId, int cantidad, LocalDateTime ahora) {
        producto.asegurarComprable();
        preventaRepository.obtenerPorProductoId(producto.getId())
                .ifPresent(preventa -> preventa.validarAcceso(ahora.toLocalDate()));
        vendedorRepository.obtenerPorId(producto.getVendedorId())
                .orElseThrow(() -> new ReglaDominioException("El vendedor del producto no existe"))
                .asegurarActivo();
        producto.validarCompraExclusiva(compraRepository.existeCompraDe(usuarioId, producto.getId()), cantidad);
        producto.asegurarStock(cantidad);
    }
}
