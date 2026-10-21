package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.RealizarCompraRequest;
import com.uniquindio.backend.application.dto.response.ConfirmarCompraResponse;
import com.uniquindio.backend.application.dto.response.RealizarCompraResponse;
import com.uniquindio.backend.application.usecase.CancelarCompraUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarCompraUseCase;
import com.uniquindio.backend.application.usecase.RealizarCompraUseCase;
import com.uniquindio.backend.application.usecase.SolicitarReembolsoUseCase;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.infrastructure.rest.mapper.CompraMapper;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController    // Maneja peticiones HTTP -> JSON
@RequestMapping("/api/compras")
public class CompraController {

    private final RealizarCompraUseCase realizarCompraUseCase;
    private final ConfirmarCompraUseCase confirmarCompraUseCase;
    private final CancelarCompraUseCase cancelarCompraUseCase;
    private final SolicitarReembolsoUseCase solicitarReembolsoUseCase;
    private final CompraMapper mapper;

    public CompraController(RealizarCompraUseCase realizarCompraUseCase,
                            ConfirmarCompraUseCase confirmarCompraUseCase,
                            CancelarCompraUseCase cancelarCompraUseCase,
                            SolicitarReembolsoUseCase solicitarReembolsoUseCase,
                            CompraMapper mapper) {
        this.realizarCompraUseCase = realizarCompraUseCase;
        this.confirmarCompraUseCase = confirmarCompraUseCase;
        this.cancelarCompraUseCase = cancelarCompraUseCase;
        this.solicitarReembolsoUseCase = solicitarReembolsoUseCase;
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

    // CU-09 Confirmar Compra
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<ConfirmarCompraResponse> confirmar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId) {
        Compra compra = confirmarCompraUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(mapper.toConfirmarResponse(compra)); // 200 OK
    }

    // CU-12 Cancelar Compra
    @PutMapping("/{id}/cancelar")
    public ResponseEntity<RealizarCompraResponse> cancelar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId) {
        Compra compra = cancelarCompraUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(mapper.toRealizarResponse(compra)); // 200 OK
    }

    // CU-13 Solicitar Reembolso
    @PutMapping("/{id}/reembolso")
    public ResponseEntity<RealizarCompraResponse> reembolsar(
            @PathVariable UUID id,
            @RequestParam UUID usuarioId) {
        Compra compra = solicitarReembolsoUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(mapper.toRealizarResponse(compra)); // 200 OK
    }
}