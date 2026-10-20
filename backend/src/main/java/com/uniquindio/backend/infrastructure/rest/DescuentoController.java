package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AplicarDescuentoRequest;
import com.uniquindio.backend.application.dto.response.ProductoDescuentoResponse;
import com.uniquindio.backend.application.usecase.AplicarDescuentoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Limitación: no hay autenticación ni autorización. El {@code productoId} viaja
 * en el body; cualquiera podría aplicar un descuento a un producto ajeno (CU-27 pendiente).
 */
@RestController
@RequestMapping("/api/descuentos")
public class DescuentoController {

    private final AplicarDescuentoUseCase aplicarDescuentoUseCase;

    public DescuentoController(AplicarDescuentoUseCase aplicarDescuentoUseCase) {
        this.aplicarDescuentoUseCase = aplicarDescuentoUseCase;
    }

    // CU-22 Configurar Descuento Promocional
    @PostMapping
    public ResponseEntity<ProductoDescuentoResponse> aplicar(@Valid @RequestBody AplicarDescuentoRequest request) {
        Producto producto = aplicarDescuentoUseCase.ejecutar(request.productoId(), request.porcentaje());
        return ResponseEntity.ok(ProductoDescuentoResponse.desde(producto));
    }
}