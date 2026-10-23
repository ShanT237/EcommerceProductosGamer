package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ProgramarPreventaRequest;
import com.uniquindio.backend.application.dto.response.PreventaResponse;
import com.uniquindio.backend.application.usecase.ProgramarPreventaUseCase;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.infrastructure.rest.mapper.PreventaMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final PreventaMapper mapper;

    public PreventaController(ProgramarPreventaUseCase programarPreventaUseCase,
                              PreventaMapper mapper) {
        this.programarPreventaUseCase = programarPreventaUseCase;
        this.mapper = mapper;
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
}