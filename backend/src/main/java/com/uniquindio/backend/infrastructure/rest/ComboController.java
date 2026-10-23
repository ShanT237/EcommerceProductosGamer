package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ComprarComboRequest;
import com.uniquindio.backend.application.dto.request.CrearComboRequest;
import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.application.dto.response.ComprarComboResponse;
import com.uniquindio.backend.application.usecase.ComprarComboUseCase;
import com.uniquindio.backend.application.usecase.CrearComboUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductosComboUseCase;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.infrastructure.rest.mapper.ComboMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * El vendedor y el comprador salen del token JWT (Authentication): un vendedor solo
 * crea y modifica sus propios combos, y el comprador solo compra a su nombre.
 */
@PreAuthorize("hasRole('VENDEDOR')")
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

    // CU-07 Crear Combo Gamer (del vendedor autenticado)
    @PostMapping
    public ResponseEntity<ComboResponse> crear(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody CrearComboRequest request) {
        Combo combo = crearComboUseCase.ejecutar(
                UUID.randomUUID(),
                vendedorId,
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

    // CU-23 Modificar Productos de Combo: Añadir (solo el vendedor dueño)
    @PutMapping("/{id}/productos/{productoId}")
    public ResponseEntity<ComboResponse> agregarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productoId,
            @AuthenticationPrincipal UUID vendedorId) {
        Combo combo = modificarProductosComboUseCase.agregar(id, vendedorId, productoId);
        return ResponseEntity.ok(mapper.toResponse(combo));
    }

    // CU-23 Modificar Productos de Combo: Quitar (solo el vendedor dueño)
    @DeleteMapping("/{id}/productos/{productoId}")
    public ResponseEntity<ComboResponse> quitarProducto(
            @PathVariable UUID id,
            @PathVariable UUID productoId,
            @AuthenticationPrincipal UUID vendedorId) {
        Combo combo = modificarProductosComboUseCase.quitar(id, vendedorId, productoId);
        return ResponseEntity.ok(mapper.toResponse(combo));
    }

    // CU-16 Comprar Combo (a nombre del usuario autenticado)
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/{id}/comprar")
    public ResponseEntity<ComprarComboResponse> comprar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody ComprarComboRequest request) {
        List<Compra> compras = comprarComboUseCase.ejecutar(usuarioId, id, request.cantidad());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toComprarResponse(id, compras)); // 201 Created
    }
}