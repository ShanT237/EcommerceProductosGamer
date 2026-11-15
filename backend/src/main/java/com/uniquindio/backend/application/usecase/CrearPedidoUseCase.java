package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.Precio;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: CrearPedido (carrito como una sola compra).
 * Genera una compra PENDIENTE por cada línea, todas con el mismo pedidoId. Aplica las mismas
 * reglas que RealizarCompraUseCase (producto comprable, preventa, vendedor activo, exclusivo,
 * promoción vigente), pero valida TODAS las líneas antes de descontar stock: si una sola
 * falla, no se modifica ningún producto ni se guarda ninguna compra.
 */
@Service
@RequiredArgsConstructor
public class CrearPedidoUseCase {

    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final PreventaRepository preventaRepository;
    private final PromocionRepository promocionRepository;
    private final Clock clock;

    @Transactional
    public List<Compra> ejecutar(UUID usuarioId, List<ItemPedido> items) {
        validarItems(items);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        // 1) Validar todo SIN modificar nada
        List<Producto> productos = new ArrayList<>();
        for (ItemPedido item : items) {
            Producto producto = productoRepository.obtenerPorId(item.productoId())
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
            validarLinea(producto, usuarioId, item.cantidad(), ahora);
            productos.add(producto);
        }

        // 2) Ya es seguro: descontar stock y armar las compras del pedido
        UUID pedidoId = UUID.randomUUID();
        List<Compra> compras = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemPedido item = items.get(i);
            Producto producto = productos.get(i);
            producto.reducirStock(item.cantidad());
            compras.add(Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(),
                    item.cantidad(), ahora, precioUnitario(producto, ahora), pedidoId));
        }

        // 3) Persistir (la transacción garantiza todo o nada)
        productos.forEach(productoRepository::guardar);
        compras.forEach(compraRepository::guardar);
        return compras;
    }

    private void validarItems(List<ItemPedido> items) {
        if (items == null || items.isEmpty()) {
            throw new ReglaDominioException("El pedido debe tener al menos un producto");
        }
        Set<UUID> vistos = new HashSet<>();
        for (ItemPedido item : items) {
            if (item == null || item.productoId() == null) {
                throw new ReglaDominioException("Cada línea del pedido debe indicar un producto");
            }
            if (item.cantidad() <= 0) {
                throw new ReglaDominioException("La cantidad de cada producto debe ser mayor a cero");
            }
            if (!vistos.add(item.productoId())) {
                throw new ReglaDominioException(
                        "El mismo producto no puede repetirse en el pedido: suma las cantidades en una sola línea");
            }
        }
    }

    private void validarLinea(Producto producto, UUID usuarioId, int cantidad, LocalDateTime ahora) {
        producto.asegurarComprable();
        preventaRepository.obtenerPorProductoId(producto.getId())
                .ifPresent(preventa -> preventa.validarAcceso(ahora.toLocalDate()));
        vendedorRepository.obtenerPorId(producto.getVendedorId())
                .orElseThrow(() -> new ReglaDominioException("El vendedor del producto no existe"))
                .asegurarActivo();
        producto.validarCompraExclusiva(compraRepository.existeCompraDe(usuarioId, producto.getId()), cantidad);
        producto.asegurarStock(cantidad);
    }

    private Precio precioUnitario(Producto producto, LocalDateTime ahora) {
        Precio precioBase = new Precio(producto.getPrecio());
        return promocionRepository
                .obtenerVigentePorProductoId(producto.getId(), ahora.toLocalDate())
                .map(promocion -> promocion.aplicarA(precioBase))
                .orElse(precioBase);
    }
}
