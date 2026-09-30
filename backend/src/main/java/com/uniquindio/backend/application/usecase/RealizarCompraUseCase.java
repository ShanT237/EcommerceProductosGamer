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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Caso de uso: RealizarCompra.
 * Recibe la intención (quién compra, qué producto, cuánto), usa los
 * repositorios para obtener/guardar datos, e invoca el comportamiento
 * del dominio (Producto, Compra) para que las reglas se apliquen solas.
 */
@Service
public class RealizarCompraUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;

    public RealizarCompraUseCase(ProductoRepository productoRepository,
                                  CompraRepository compraRepository,
                                  UsuarioRepository usuarioRepository,
                                  VendedorRepository vendedorRepository) {
        this.productoRepository = productoRepository;
        this.compraRepository = compraRepository;
        this.usuarioRepository = usuarioRepository;
        this.vendedorRepository = vendedorRepository;
    }

    public RealizarCompraUseCase(ProductoRepository productoRepository, CompraRepository compraRepository) {
        this(productoRepository, compraRepository, null, null);
    }

    public Compra ejecutar(UUID id, UUID usuarioId, UUID productoId, int cantidad, LocalDate fecha) {
        if (usuarioRepository != null) {
            Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                    .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
            if (!usuario.isActivo()) {
                throw new ReglaDominioException("El usuario se encuentra inactivo");
            }
        }

        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new ReglaDominioException("El producto no existe"));

        if (producto.isEliminado()) {
            throw new ReglaDominioException("El producto no se encuentra disponible (eliminado)");
        }

        if (vendedorRepository != null) {
            Optional<Vendedor> vendedorOpt = vendedorRepository.obtenerPorId(producto.getVendedorId());
            if (vendedorOpt.isPresent() && !vendedorOpt.get().isActivo()) {
                throw new ReglaDominioException("El vendedor del producto no se encuentra activo");
            }
        }

        boolean usuarioYaTieneEsteProducto = compraRepository.existeCompraDe(usuarioId, productoId);

        producto.validarCompraExclusiva(usuarioYaTieneEsteProducto); // Regla 9
        producto.reducirStock(cantidad);                             // Regla 1

        Compra compra = new Compra(id, usuarioId, productoId, cantidad, fecha, new Precio(producto.getPrecio()));

        productoRepository.guardar(producto);
        compraRepository.guardar(compra);

        return compra;
    }
}