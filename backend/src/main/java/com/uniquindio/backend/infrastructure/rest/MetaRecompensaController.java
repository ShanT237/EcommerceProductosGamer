package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.ActualizarUmbralMetaRequest;
import com.uniquindio.backend.application.dto.request.CrearMetaRecompensaRequest;
import com.uniquindio.backend.application.dto.response.MetaRecompensaResponse;
import com.uniquindio.backend.application.usecase.GestionarMetasRecompensaUseCase;
import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.infrastructure.rest.mapper.MetaRecompensaMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Limitación: es una operación de admin, pero no hay roles. Cualquier usuario con JWT
 * válido podría crear, cambiar o desactivar metas.
 */
@PreAuthorize("hasRole('VENDEDOR')")
@RestController
@RequestMapping("/api/metas-recompensa")
public class MetaRecompensaController {

    private final GestionarMetasRecompensaUseCase gestionarMetasUseCase;
    private final MetaRecompensaMapper mapper;

    public MetaRecompensaController(GestionarMetasRecompensaUseCase gestionarMetasUseCase,
                                    MetaRecompensaMapper mapper) {
        this.gestionarMetasUseCase = gestionarMetasUseCase;
        this.mapper = mapper;
    }

    // CU-33 Gestionar Metas: crear
    @PostMapping
    public ResponseEntity<MetaRecompensaResponse> crear(
            @Valid @RequestBody CrearMetaRecompensaRequest request) {
        MetaRecompensa meta = gestionarMetasUseCase.crear(
                UUID.randomUUID(),
                request.comprasRequeridas(),
                request.productoRegaloId()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(meta.getId())
                .toUri();

        return ResponseEntity.created(location).body(mapper.toResponse(meta)); // 201 Created
    }

    // CU-33 Gestionar Metas: cambiar umbral
    @PutMapping("/{id}/umbral")
    public ResponseEntity<MetaRecompensaResponse> actualizarUmbral(
            @PathVariable UUID id,
            @Valid @RequestBody ActualizarUmbralMetaRequest request) {
        MetaRecompensa meta = gestionarMetasUseCase.actualizarUmbral(id, request.nuevoUmbral());
        return ResponseEntity.ok(mapper.toResponse(meta)); // 200 OK
    }

    // CU-33 Gestionar Metas: desactivar (nunca se borra)
    @PutMapping("/{id}/desactivar")
    public ResponseEntity<MetaRecompensaResponse> desactivar(@PathVariable UUID id) {
        MetaRecompensa meta = gestionarMetasUseCase.desactivar(id);
        return ResponseEntity.ok(mapper.toResponse(meta)); // 200 OK
    }
}