package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Caso de uso: ExpirarComprasPendientes (automático, lo dispara el scheduler; no hay endpoint).
 * Una compra PENDIENTE expira cuando su fecha es estrictamente anterior a {@code ahora - 10 min}.
 * Si pertenece a un pedido o combo expira el pedido completo (todas sus líneas PENDIENTES), igual
 * que {@link CancelarPedidoUseCase}: se restituye el stock de cada línea y se avisa de stock repuesto.
 * No restituye cupones (igual que cancelar).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpirarComprasPendientesUseCase {

    public static final Duration TIEMPO_MAXIMO_PENDIENTE = Duration.ofMinutes(10);

    private final CompraRepository compraRepository;
    private final ProductoRepository productoRepository;
    private final Clock clock;
    private final NotificadorEventos notificador;

    /** @return número de compras expiradas */
    @Transactional
    public int ejecutar() {
        LocalDateTime limite = LocalDateTime.now(clock).minus(TIEMPO_MAXIMO_PENDIENTE);

        // Una compra suelta es un grupo de una; las líneas de un pedido forman un solo grupo.
        Map<UUID, List<Compra>> grupos = new LinkedHashMap<>();
        for (Compra compra : compraRepository.obtenerPendientesAnterioresA(limite)) {
            UUID clave = compra.perteneceAUnPedido() ? compra.getPedidoId() : compra.getId();
            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(compra);
        }

        int expiradas = 0;
        for (Map.Entry<UUID, List<Compra>> grupo : grupos.entrySet()) {
            List<Compra> lineas = grupo.getValue();
            if (lineas.get(0).perteneceAUnPedido()) {
                // El pedido completo, pero solo lo que sigue PENDIENTE (otras líneas pudieron confirmarse).
                lineas = compraRepository.obtenerPorPedidoId(grupo.getKey()).stream()
                        .filter(Compra::estaPendiente)
                        .toList();
            }
            expiradas += expirarGrupo(lineas);
        }
        return expiradas;
    }

    private int expirarGrupo(List<Compra> lineas) {
        // Se verifica la existencia de todos los productos ANTES de mutar nada. Un producto que
        // falta deja el grupo intacto (y se registra) en vez de abortar y bloquear al resto.
        Map<UUID, Producto> productos = new LinkedHashMap<>();
        for (Compra compra : lineas) {
            if (!productos.containsKey(compra.getProductoId())) {
                Producto producto = productoRepository.obtenerPorId(compra.getProductoId()).orElse(null);
                if (producto == null) {
                    log.warn("No se expiró la compra {}: el producto {} no existe",
                            compra.getId(), compra.getProductoId());
                    return 0;
                }
                productos.put(compra.getProductoId(), producto);
            }
        }

        // Una sola instancia por producto: dos líneas del mismo producto suman sus cantidades.
        List<Integer> stocksAnteriores = new ArrayList<>();
        for (Compra compra : lineas) {
            Producto producto = productos.get(compra.getProductoId());
            compra.expirar();
            stocksAnteriores.add(producto.getStock());
            producto.aumentarStock(compra.getCantidad());
        }

        lineas.forEach(compraRepository::guardar);
        productos.values().forEach(productoRepository::guardar);
        for (int i = 0; i < lineas.size(); i++) {
            notificador.stockRepuesto(productos.get(lineas.get(i).getProductoId()), stocksAnteriores.get(i));
        }
        return lineas.size();
    }
}
