package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CrearComboRequest;
import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.application.usecase.CrearComboUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductosComboUseCase;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.infrastructure.rest.mapper.ComboMapper;
import com.uniquindio.backend.application.dto.request.ComprarComboRequest;
import com.uniquindio.backend.application.dto.response.ComprarComboResponse;
import com.uniquindio.backend.application.usecase.ComprarComboUseCase;
import com.uniquindio.backend.domain.entity.Compra;
import org.springframework.http.HttpStatus;


import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.net.URI;
import java.util.UUID;

/**
 * Limitación: no hay autenticación ni autorización. El {@code vendedorId} viaja
 * en el body; cualquiera podría crear o modificar combos de otro vendedor (CU-27 pendiente).
 */
@RestController
@RequestMapping("/api/combos")
public class ComboController {

    private final CrearComboUseCase crearComboUseCase;
    private final ModificarProductosComboUseCase modificarProductosComboUseCase;
    private final ComprarComboUseCase comprarComboUseCase;
    private final ComboMapper mapper;

    public ComboController(CrearComboUseCase crearComboUseCase,
                           ModificarProductosComboUseCase modificarProductosComboUseCase,
                           ComprarComboUseCase comprarComboUseCase,
                           ComboMapper mapper) {
        this.crearComboUseCase = crearComboUseCase;
        this.modificarProductosComboUseCase = modificarProductosComboUseCase;
        this.comprarComboUseCase = comprarComboUseCase;
        this.mapper = mapper;
    }

    // CU-07 Crear Combo Gamer
    @PostMapping
    public ResponseEntity<ComboResponse> crear(@Valid @RequestBody CrearComboRequest request) {
        UUID comboId = UUID.randomUUID();
        Combo combo = crearComboUseCase.ejecutar(
                comboId,
                request.vendedorId(),
                request.nombre(),
                request.descripcion(),
                request.descuento(),
                request.productoIds()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(combo.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(combo));
    }

    // CU-23 Modificar Productos de Combo: Añadir
    @PutMapping("/{id}/productos/{productoId}")
    public ResponseEntity<ComboResponse> agregarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productoId) {
        Combo combo = modificarProductosComboUseCase.agregar(id, productoId);
        return ResponseEntity.ok(mapper.toResponse(combo));
    }

    // CU-23 Modificar Productos de Combo: Quitar
    @DeleteMapping("/{id}/productos/{productoId}")
    public ResponseEntity<ComboResponse> quitarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productoId) {
        Combo combo = modificarProductosComboUseCase.quitar(id, productoId);
        return ResponseEntity.ok(mapper.toResponse(combo));
    }

    // CU-16 Comprar Combo
    @PostMapping("/{id}/comprar")
    public ResponseEntity<ComprarComboResponse> comprar(
            @PathVariable UUID id,
            @Valid @RequestBody ComprarComboRequest request) {
        List<Compra> compras = comprarComboUseCase.ejecutar(request.usuarioId(), id, request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toComprarResponse(id, compras)); // 201 Created
    }
}