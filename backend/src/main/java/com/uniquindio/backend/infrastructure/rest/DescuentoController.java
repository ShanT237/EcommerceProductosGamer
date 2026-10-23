package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AplicarDescuentoRequest;
import com.uniquindio.backend.application.dto.response.ProductoDescuentoResponse;
import com.uniquindio.backend.application.usecase.AplicarDescuentoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * El {@code vendedorId} sale del token JWT (Authentication): el caso de uso verifica
 * que el producto pertenezca a ese vendedor antes de aplicar el descuento.
 */
@PreAuthorize("hasRole('VENDEDOR')")
@RestController
@RequestMapping("/api/descuentos")
public class DescuentoController {

    private final AplicarDescuentoUseCase aplicarDescuentoUseCase;

    public DescuentoController(AplicarDescuentoUseCase aplicarDescuentoUseCase) {
        this.aplicarDescuentoUseCase = aplicarDescuentoUseCase;
    }

    // CU-22 Configurar Descuento Promocional (sobre un producto del vendedor autenticado)
    @PostMapping
    public ResponseEntity<ProductoDescuentoResponse> aplicar(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody AplicarDescuentoRequest request) {
        Producto producto = aplicarDescuentoUseCase.ejecutar(
                request.productoId(), vendedorId, request.porcentaje());
        return ResponseEntity.ok(ProductoDescuentoResponse.desde(producto)); // 200 OK
    }
}