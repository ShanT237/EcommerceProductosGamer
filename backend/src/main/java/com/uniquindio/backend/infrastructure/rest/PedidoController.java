package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CrearPedidoRequest;
import com.uniquindio.backend.application.dto.response.PedidoResponse;
import com.uniquindio.backend.application.usecase.CancelarPedidoUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarPedidoUseCase;
import com.uniquindio.backend.application.usecase.CrearPedidoUseCase;
import com.uniquindio.backend.application.usecase.ItemPedido;
import com.uniquindio.backend.domain.entity.Compra;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Pedido = el carrito convertido en una sola operación: se crea, se confirma o se cancela
 * completo. El comprador sale siempre del token JWT, nunca del body ni de la URL.
 */
@PreAuthorize("hasRole('COMPRADOR')")
@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final CrearPedidoUseCase crearPedidoUseCase;
    private final ConfirmarPedidoUseCase confirmarPedidoUseCase;
    private final CancelarPedidoUseCase cancelarPedidoUseCase;

    public PedidoController(CrearPedidoUseCase crearPedidoUseCase,
                            ConfirmarPedidoUseCase confirmarPedidoUseCase,
                            CancelarPedidoUseCase cancelarPedidoUseCase) {
        this.crearPedidoUseCase = crearPedidoUseCase;
        this.confirmarPedidoUseCase = confirmarPedidoUseCase;
        this.cancelarPedidoUseCase = cancelarPedidoUseCase;
    }

    // Crear pedido: una compra PENDIENTE por línea, con stock descontado de todas o de ninguna.
    // Con codigoCupon, el cupón se valida una vez y consume un solo uso para todo el pedido.
    @PostMapping
    public ResponseEntity<PedidoResponse> crear(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody CrearPedidoRequest request) {
        List<ItemPedido> items = request.items().stream()
                .map(item -> new ItemPedido(item.productoId(), item.cantidad()))
                .toList();
        List<Compra> compras = crearPedidoUseCase.ejecutar(usuarioId, items, request.codigoCupon());
        PedidoResponse response = PedidoResponse.desde(compras);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.pedidoId())
                .toUri();
        return ResponseEntity.created(location).body(response); // 201 = created
    }

    // Confirmar el pago de todas las líneas del pedido
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<PedidoResponse> confirmar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        List<Compra> compras = confirmarPedidoUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(PedidoResponse.desde(compras)); // 200 OK
    }

    // Cancelar el pedido completo y devolver el stock
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        List<Compra> compras = cancelarPedidoUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(PedidoResponse.desde(compras)); // 200 OK
    }
}
