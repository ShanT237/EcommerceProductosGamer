package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ConfigurarDescuentoPromocionalRequest;
import com.uniquindio.backend.application.dto.response.PromocionResponse;
import com.uniquindio.backend.application.usecase.ConfigurarDescuentoPromocionalUseCase;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.infrastructure.rest.mapper.PromocionMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Limitación: el {@code vendedorId} viaja en el body; el JWT valida la sesión pero
 * no se cruza con el vendedor dueño del producto.
 */
@RestController
@RequestMapping("/api/promociones")
public class PromocionController {

    private final ConfigurarDescuentoPromocionalUseCase configurarDescuentoPromocionalUseCase;
    private final PromocionMapper mapper;

    public PromocionController(ConfigurarDescuentoPromocionalUseCase configurarDescuentoPromocionalUseCase,
                               PromocionMapper mapper) {
        this.configurarDescuentoPromocionalUseCase = configurarDescuentoPromocionalUseCase;
        this.mapper = mapper;
    }

    // CU-22 Configurar Descuento Promocional (con vigencia; no cambia el precio base)
    @PostMapping
    public ResponseEntity<PromocionResponse> configurar(
            @Valid @RequestBody ConfigurarDescuentoPromocionalRequest request) {
        Promocion promocion = configurarDescuentoPromocionalUseCase.ejecutar(
                request.productoId(),
                request.vendedorId(),
                request.porcentaje(),
                request.desde(),
                request.hasta()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(promocion.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(promocion)); // 201 Created
    }
}