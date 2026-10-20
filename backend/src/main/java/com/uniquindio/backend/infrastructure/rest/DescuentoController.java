package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AplicarDescuentoRequest;
import com.uniquindio.backend.application.dto.response.ProductoDescuentoResponse;
import com.uniquindio.backend.application.usecase.AplicarDescuentoUseCase;
import com.uniquindio.backend.domain.entity.Producto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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