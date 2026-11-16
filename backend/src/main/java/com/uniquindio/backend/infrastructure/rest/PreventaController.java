package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ModificarPreventaRequest;
import com.uniquindio.backend.application.dto.request.ProgramarPreventaRequest;
import com.uniquindio.backend.application.dto.response.PreventaResponse;
import com.uniquindio.backend.application.usecase.CancelarPreventaUseCase;
import com.uniquindio.backend.application.usecase.ConsultarPreventasUseCase;
import com.uniquindio.backend.application.usecase.ModificarPreventaUseCase;
import com.uniquindio.backend.application.usecase.ProgramarPreventaUseCase;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.infrastructure.rest.mapper.PreventaMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * El {@code vendedorId} sale del token JWT (Authentication): el caso de uso verifica
 * que el vendedor esté activo y que el producto sea suyo.
 */
@PreAuthorize("hasRole('VENDEDOR')")
@RestController
@RequestMapping("/api/preventas")
public class PreventaController {

    private final ProgramarPreventaUseCase programarPreventaUseCase;
    private final ConsultarPreventasUseCase consultarPreventasUseCase;
    private final ModificarPreventaUseCase modificarPreventaUseCase;
    private final CancelarPreventaUseCase cancelarPreventaUseCase;
    private final PreventaMapper mapper;

    public PreventaController(ProgramarPreventaUseCase programarPreventaUseCase,
                              ConsultarPreventasUseCase consultarPreventasUseCase,
                              ModificarPreventaUseCase modificarPreventaUseCase,
                              CancelarPreventaUseCase cancelarPreventaUseCase,
                              PreventaMapper mapper) {
        this.programarPreventaUseCase = programarPreventaUseCase;
        this.consultarPreventasUseCase = consultarPreventasUseCase;
        this.modificarPreventaUseCase = modificarPreventaUseCase;
        this.cancelarPreventaUseCase = cancelarPreventaUseCase;
        this.mapper = mapper;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<java.util.List<PreventaResponse>> listar() {
        return ResponseEntity.ok(consultarPreventasUseCase.ejecutar().stream()
                .map(mapper::toResponse)
                .toList());
    }

    // CU-37 Programar Preventa (fecha de lanzamiento futura, una por producto)
    @PostMapping
    public ResponseEntity<PreventaResponse> programar(
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ProgramarPreventaRequest request) {
        Preventa preventa = programarPreventaUseCase.ejecutar(
                vendedorId,
                request.productoId(),
                request.fechaLanzamiento(),
                request.descripcion()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(preventa.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(preventa)); // 201 Created
    }

    // Modificar fecha de lanzamiento y descripción (solo mientras la preventa no se haya lanzado)
    @PutMapping("/{id}")
    public ResponseEntity<PreventaResponse> modificar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID vendedorId,
            @Valid @RequestBody ModificarPreventaRequest request) {
        Preventa preventa = modificarPreventaUseCase.ejecutar(
                id, vendedorId, request.fechaLanzamiento(), request.descripcion());
        return ResponseEntity.ok(mapper.toResponse(preventa)); // 200
    }

    // Cancelar la preventa (solo mientras no se haya lanzado)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id, @AuthenticationPrincipal UUID vendedorId) {
        cancelarPreventaUseCase.ejecutar(id, vendedorId);
        return ResponseEntity.noContent().build(); // 204
    }
}
