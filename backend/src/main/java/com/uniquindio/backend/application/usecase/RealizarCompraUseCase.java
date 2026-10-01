package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    public Compra ejecutar(UUID id, UUID usuarioId, UUID productoId, int cantidad, LocalDate fecha) {
        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
        producto.asegurarNoEliminado();

        Vendedor vendedor = vendedorRepository.obtenerPorId(producto.getVendedorId())
                .orElseThrow(() -> new ReglaDominioException("El vendedor del producto no existe"));
        vendedor.asegurarActivo();

        boolean usuarioYaTieneEsteProducto = compraRepository.existeCompraDe(usuarioId, productoId);
        producto.validarCompraExclusiva(usuarioYaTieneEsteProducto);

        producto.reducirStock(cantidad);

        Compra compra = new Compra(id, usuarioId, productoId, cantidad, fecha, new Precio(producto.getPrecio()));

        productoRepository.guardar(producto);
        compraRepository.guardar(compra);

        return compra;
    }
}
