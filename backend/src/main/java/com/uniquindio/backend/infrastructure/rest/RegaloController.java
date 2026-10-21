package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AsignarRegaloRequest;
import com.uniquindio.backend.application.dto.response.RegaloResponse;
import com.uniquindio.backend.application.usecase.AsignarRegaloPorMetaUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.rest.mapper.RegaloMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Limitación: no hay control de roles. Cualquier usuario con JWT podría asignar
 * regalos (CU-10 es de Admin / Sistema).
 */
@RestController
@RequestMapping("/api/regalos")
public class RegaloController {

    private final AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase;
    private final RegaloMapper mapper;

    public RegaloController(AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase,
                            RegaloMapper mapper) {
        this.asignarRegaloPorMetaUseCase = asignarRegaloPorMetaUseCase;
        this.mapper = mapper;
    }

    // CU-10 Asignar Regalo por Meta (5 compras COMPLETADAS por regalo)
    @PutMapping("/{id}/asignar")
    public ResponseEntity<RegaloResponse> asignar(
            @PathVariable UUID id,
            @Valid @RequestBody AsignarRegaloRequest request) {
        Regalo regalo = asignarRegaloPorMetaUseCase.ejecutar(request.usuarioId(), id);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }
}