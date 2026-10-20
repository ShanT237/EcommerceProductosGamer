package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.*;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Caso de uso: RealizarCompra.
 * Orquesta la compra: valida usuario, producto y vendedor, aplica las reglas
 * de la entidad y persiste a través de los repositorios.
 */
@Service
@RequiredArgsConstructor
public class RealizarCompraUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final PreventaRepository preventaRepository;
    private final Clock clock;

    @Transactional
    public Compra ejecutar(UUID usuarioId, UUID productoId, int cantidad) {
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();

        preventaRepository.obtenerPorProductoId(productoId)
                .ifPresent(preventa -> preventa.validarAcceso(ahora.toLocalDate()));

        Vendedor vendedor = vendedorRepository.obtenerPorId(producto.getVendedorId())
                .orElseThrow(() -> new ReglaDominioException("El vendedor del producto no existe"));
        vendedor.asegurarActivo();

        boolean yaLoTiene = compraRepository.existeCompraDe(usuarioId, productoId);
        producto.validarCompraExclusiva(yaLoTiene, cantidad);

        producto.reducirStock(cantidad);

        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, productoId, cantidad,
                ahora, new Precio(producto.getPrecio()));

        productoRepository.guardar(producto);
        compraRepository.guardar(compra);
        return compra;
    }
}
