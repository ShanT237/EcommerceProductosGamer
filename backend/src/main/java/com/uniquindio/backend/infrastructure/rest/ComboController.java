package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ComprarComboRequest;
import com.uniquindio.backend.application.dto.request.CrearComboRequest;
import com.uniquindio.backend.application.dto.request.ModificarComboRequest;
import com.uniquindio.backend.application.dto.response.ComboDetalleResponse;
import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.application.dto.response.ComprarComboResponse;
import com.uniquindio.backend.application.usecase.ComprarComboUseCase;
import com.uniquindio.backend.application.usecase.ConsultarCombosUseCase;
import com.uniquindio.backend.application.usecase.CrearComboUseCase;
import com.uniquindio.backend.application.usecase.EliminarComboUseCase;
import com.uniquindio.backend.application.usecase.ModificarComboUseCase;
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
    private final ModificarComboUseCase modificarComboUseCase;
    private final EliminarComboUseCase eliminarComboUseCase;
    private final ModificarProductosComboUseCase modificarProductosComboUseCase;
    private final ComprarComboUseCase comprarComboUseCase;
    private final ConsultarCombosUseCase consultarCombosUseCase;
    private final ComboMapper mapper;

    public ComboController(CrearComboUseCase crearComboUseCase,
                           ModificarComboUseCase modificarComboUseCase,
                           EliminarComboUseCase eliminarComboUseCase,
                           ModificarProductosComboUseCase modificarProductosComboUseCase,
                           ComprarComboUseCase comprarComboUseCase,
                           ConsultarCombosUseCase consultarCombosUseCase,
                           ComboMapper mapper) {
        this.crearComboUseCase = crearComboUseCase;
        this.modificarComboUseCase = modificarComboUseCase;
        this.eliminarComboUseCase = eliminarComboUseCase;
        this.modificarProductosComboUseCase = modificarProductosComboUseCase;
        this.comprarComboUseCase = comprarComboUseCase;
        this.consultarCombosUseCase = consultarCombosUseCase;
        this.mapper = mapper;
    }

    // Catálogo de combos comprables (cualquier sesión: comprador o vendedor)
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<List<ComboDetalleResponse>> listar() {
        return ResponseEntity.ok(mapper.toDetalleResponses(consultarCombosUseCase.todos()));
    }

    // Combos del vendedor autenticado (incluye los que tienen productos pausados)
    @PreAuthorize("hasRole('VENDEDOR')")
    @GetMapping("/mios")
    public ResponseEntity<List<ComboDetalleResponse>> mios(@AuthenticationPrincipal UUID vendedorId) {
        return ResponseEntity.ok(mapper.toDetalleResponses(consultarCombosUseCase.deVendedor(vendedorId)));
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

    // Modificar nombre, descripción y descuento (solo el vendedor dueño)
    @PutMapping("/{id}")
    public ResponseEntity<ComboResponse> modificar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ModificarComboRequest request) {
        Combo combo = modificarComboUseCase.ejecutar(
                id, vendedorId, request.nombre(), request.descripcion(), request.descuento());
        return ResponseEntity.ok(mapper.toResponse(combo));
    }

    // Eliminar el combo (solo el vendedor dueño)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId) {
        eliminarComboUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.noContent().build(); // 204 No Content
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
