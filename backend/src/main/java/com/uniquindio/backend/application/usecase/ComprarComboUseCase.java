package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: ComprarCombo (CU-16).
 * Genera una compra PENDIENTE por cada producto del combo, con el descuento del combo
 * aplicado al precio base y un único pedidoId para operar el combo como una unidad.
 * Un cupón opcional se aplica después del descuento del combo, sin acumular promociones
 * individuales. Todo se valida ANTES de descontar stock.
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
    private final CuponRepository cuponRepository;
    private final Clock clock;

    @Transactional
    public List<Compra> ejecutar(UUID usuarioId, UUID comboId, int cantidad) {
        return ejecutar(usuarioId, comboId, cantidad, null);
    }

    @Transactional
    public List<Compra> ejecutar(UUID usuarioId, UUID comboId, int cantidad, String codigoCupon) {
        if (cantidad <= 0) {
            throw new ReglaDominioException("La cantidad del combo debe ser mayor a cero");
        }
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        Combo combo = comboRepository.obtenerPorId(comboId)
                .orElseThrow(() -> new NoSuchElementException("El combo no existe"));

        Cupon cupon = obtenerCuponValido(codigoCupon, usuarioId, ahora);
        if (cupon != null && !cupon.aplicaA(combo.getVendedorId())) {
            throw new ReglaDominioException("El cupón no aplica a este combo");
        }

        List<Producto> productos = new ArrayList<>();
        for (UUID productoId : combo.getProductos()) {
            Producto producto = productoRepository.obtenerPorId(productoId)
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
            validarProducto(producto, usuarioId, cantidad, ahora);
            productos.add(producto);
        }

        UUID pedidoId = UUID.randomUUID();
        List<Compra> compras = new ArrayList<>();
        for (Producto producto : productos) {
            producto.reducirStock(cantidad);
            var precioUnitario = combo.calcularPrecioUnitario(producto);
            Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), cantidad,
                    ahora, precioUnitario, pedidoId, combo.getId(), combo.getNombre());
            if (cupon != null) {
                compra.aplicarCupon(cupon);
            }
            compras.add(compra);
        }
        if (cupon != null) {
            cupon.registrarUso();
        }

        productos.forEach(productoRepository::guardar);
        compras.forEach(compraRepository::guardar);
        if (cupon != null) {
            cuponRepository.guardar(cupon);
            cuponRepository.registrarUsoDe(cupon.getId(), usuarioId);
        }
        return compras;
    }

    private Cupon obtenerCuponValido(String codigoCupon, UUID usuarioId, LocalDateTime ahora) {
        if (codigoCupon == null || codigoCupon.isBlank()) {
            return null;
        }
        Cupon cupon = cuponRepository.obtenerPorCodigo(codigoCupon)
                .orElseThrow(() -> new NoSuchElementException("El cupón no existe"));
        cupon.validarUso(ahora.toLocalDate());
        if (cuponRepository.fueUsadoPor(cupon.getId(), usuarioId)) {
            throw new ReglaDominioException("Ya usaste este cupón: solo se puede usar una vez por comprador");
        }
        return cupon;
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
