package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RealizarCompraRequest;
import com.uniquindio.backend.application.dto.response.RealizarCompraResponse;
import com.uniquindio.backend.application.usecase.RealizarCompraUseCase;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.infrastructure.rest.mapper.CompraMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/compras")
public class CompraController {

    private final RealizarCompraUseCase realizarCompraUseCase;
    private final CompraMapper mapper;

    public CompraController(RealizarCompraUseCase realizarCompraUseCase,
                            CompraMapper mapper) {
        this.realizarCompraUseCase = realizarCompraUseCase;
        this.mapper = mapper;
    }

    // CU-01 Realizar Compra
    @PostMapping
    public ResponseEntity<RealizarCompraResponse> realizar(
            @Valid @RequestBody RealizarCompraRequest request) {
        Compra compra = realizarCompraUseCase.ejecutar(
                request.usuarioId(),
                request.productoId(),
                request.cantidad()
        );

        RealizarCompraResponse response = mapper.toRealizarResponse(compra);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(compra.getId())
                .toUri();
        return ResponseEntity.created(location).body(response); // 201 = created
    }
}