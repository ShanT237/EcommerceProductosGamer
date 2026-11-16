package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.CuponRepository;
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
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

/**
 * Caso de uso: CrearPedido (carrito como una sola compra).
 * Genera una compra PENDIENTE por cada línea, todas con el mismo pedidoId. Aplica las mismas
 * reglas que RealizarCompraUseCase (producto comprable, preventa, vendedor activo, exclusivo,
 * promoción vigente), pero valida TODAS las líneas antes de descontar stock: si una sola
 * falla, no se modifica ningún producto ni se guarda ninguna compra.
 * Cupón opcional: se valida una vez, se aplica solo a las líneas de productos a los que aplica
 * (todos si es global; los de su vendedor si tiene dueño) y consume UN solo uso por pedido,
 * sin importar cuántas líneas tenga. Cada comprador puede usar un mismo cupón una sola vez.
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
    private final CuponRepository cuponRepository;
    private final Clock clock;
    private final NotificadorEventos notificador;

    @Transactional
    public List<Compra> ejecutar(UUID usuarioId, List<ItemPedido> items, String codigoCupon) {
        validarItems(items);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Usuario usuario = usuarioRepository.obtenerPorId(usuarioId)
                .orElseThrow(() -> new ReglaDominioException("El usuario no existe"));
        usuario.asegurarActivo();

        // 1) Validar todo SIN modificar nada
        Cupon cupon = obtenerCuponValido(codigoCupon, usuarioId, ahora);
        List<Producto> productos = new ArrayList<>();
        for (ItemPedido item : items) {
            Producto producto = productoRepository.obtenerPorId(item.productoId())
                    .orElseThrow(() -> new ReglaDominioException("El producto no existe"));
            validarLinea(producto, usuarioId, item.cantidad(), ahora);
            productos.add(producto);
        }
        if (cupon != null && productos.stream().noneMatch(p -> cupon.aplicaA(p.getVendedorId()))) {
            throw new ReglaDominioException("El cupón no aplica a ningún producto del pedido");
        }

        // 2) Ya es seguro: descontar stock y armar las compras del pedido
        UUID pedidoId = UUID.randomUUID();
        List<Compra> compras = new ArrayList<>();
        List<Integer> stocksAnteriores = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemPedido item = items.get(i);
            Producto producto = productos.get(i);
            stocksAnteriores.add(producto.getStock());
            producto.reducirStock(item.cantidad());
            Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(),
                    item.cantidad(), ahora, precioUnitario(producto, ahora), pedidoId);
            if (cupon != null && cupon.aplicaA(producto.getVendedorId())) {
                compra.aplicarCupon(cupon);
            }
            compras.add(compra);
        }
        if (cupon != null) {
            cupon.registrarUso();   // un solo uso por pedido, no uno por línea
        }

        // 3) Persistir (la transacción garantiza todo o nada)
        productos.forEach(productoRepository::guardar);
        compras.forEach(compraRepository::guardar);
        if (cupon != null) {
            cuponRepository.guardar(cupon);
            cuponRepository.registrarUsoDe(cupon.getId(), usuarioId);
        }
        for (int i = 0; i < items.size(); i++) {
            notificador.nuevaVenta(productos.get(i), items.get(i).cantidad());
            notificador.stockDelVendedor(productos.get(i), stocksAnteriores.get(i));
        }
        return compras;
    }

    /** Sin código no hay cupón; con código debe existir, poder usarse hoy y no haber sido usado ya por el comprador. */
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
