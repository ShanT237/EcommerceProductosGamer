package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.CrearComboRequest;
import com.uniquindio.backend.application.dto.response.ComboResponse;
import com.uniquindio.backend.application.usecase.CrearComboUseCase;
import com.uniquindio.backend.application.usecase.ModificarProductosComboUseCase;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.infrastructure.rest.mapper.ComboMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/combos")
public class ComboController {

    private final CrearComboUseCase crearComboUseCase;
    private final ModificarProductosComboUseCase modificarProductosComboUseCase;
    private final ComboMapper mapper;

    public ComboController(CrearComboUseCase crearComboUseCase,
                           ModificarProductosComboUseCase modificarProductosComboUseCase,
                           ComboMapper mapper) {
        this.crearComboUseCase = crearComboUseCase;
        this.modificarProductosComboUseCase = modificarProductosComboUseCase;
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
}