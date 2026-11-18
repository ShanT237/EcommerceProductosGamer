package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.ArmadorResumenChat;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.AccesoProhibidoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.TipoContextoChat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Inicia (o reutiliza) la conversación entre el comprador y el vendedor de un producto o de una orden.
 * Es idempotente: si ya existe una conversación para ese contexto, la devuelve.
 */
@Service
@RequiredArgsConstructor
public class IniciarConversacionChatUseCase {

    private final ConversacionChatRepository conversacionRepository;
    private final MensajeChatRepository mensajeRepository;
    private final ProductoRepository productoRepository;
    private final CompraRepository compraRepository;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final ArmadorResumenChat armador;
    private final Clock clock;

    @Transactional
    public ResumenConversacionChat ejecutar(UUID compradorId, TipoContextoChat tipo,
                                            UUID referenciaId, UUID vendedorId) {
        usuarioRepository.obtenerPorId(compradorId)
                .orElseThrow(() -> new NoSuchElementException("El usuario no existe"))
                .asegurarActivo();

        UUID vendedorDestino = switch (tipo) {
            case PRODUCTO -> vendedorDelProducto(referenciaId);
            case ORDEN -> vendedorDeLaOrden(compradorId, referenciaId, vendedorId);
        };

        vendedorRepository.obtenerPorId(vendedorDestino)
                .orElseThrow(() -> new NoSuchElementException("El vendedor no existe"))
                .asegurarActivo();

        Optional<ConversacionChat> existente =
                conversacionRepository.obtenerPorContexto(compradorId, vendedorDestino, tipo, referenciaId);
        if (existente.isPresent()) {
            long noLeidos = mensajeRepository.contarNoLeidosPorConversacion(compradorId)
                    .getOrDefault(existente.get().getId(), 0L);
            return armador.armar(existente.get(), compradorId, noLeidos);
        }

        ConversacionChat nueva = ConversacionChat.iniciar(UUID.randomUUID(), compradorId, vendedorDestino,
                tipo, referenciaId, LocalDateTime.now(clock));
        conversacionRepository.guardar(nueva);
        return armador.armar(nueva, compradorId, 0);
    }

    // El vendedor sale siempre del producto: se ignora el vendedorId del request.
    private UUID vendedorDelProducto(UUID productoId) {
        Producto producto = productoRepository.obtenerPorId(productoId)
                .orElseThrow(() -> new NoSuchElementException("El producto no existe"));
        if (producto.isEliminado()) {
            throw new ReglaDominioException("El producto ya no está disponible");
        }
        return producto.getVendedorId();
    }

    private UUID vendedorDeLaOrden(UUID compradorId, UUID operacionId, UUID vendedorSolicitado) {
        // El frontend usa pedidoId ?? compra.id como identificador de la operación.
        List<Compra> compras = compraRepository.obtenerPorPedidoId(operacionId);
        if (compras.isEmpty()) {
            compras = compraRepository.obtenerPorId(operacionId).map(List::of).orElse(List.of());
        }
        if (compras.isEmpty()) {
            throw new NoSuchElementException("La orden no existe");
        }
        if (compras.stream().anyMatch(c -> !compradorId.equals(c.getUsuarioId()))) {
            throw new AccesoProhibidoException("La orden no te pertenece");
        }

        Set<UUID> vendedoresDeLaOrden = new LinkedHashSet<>();
        for (Compra compra : compras) {
            productoRepository.obtenerPorId(compra.getProductoId())
                    .ifPresent(p -> vendedoresDeLaOrden.add(p.getVendedorId()));
        }
        if (vendedoresDeLaOrden.isEmpty()) {
            throw new ReglaDominioException("La orden no tiene vendedores asociados");
        }

        if (vendedorSolicitado == null) {
            if (vendedoresDeLaOrden.size() > 1) {
                throw new ReglaDominioException(
                        "La orden tiene varios vendedores: indica con cuál quieres chatear");
            }
            return vendedoresDeLaOrden.iterator().next();
        }
        if (!vendedoresDeLaOrden.contains(vendedorSolicitado)) {
            throw new ReglaDominioException("El vendedor indicado no pertenece a la orden");
        }
        return vendedorSolicitado;
    }
}
