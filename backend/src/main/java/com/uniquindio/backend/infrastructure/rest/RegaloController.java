package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.RegaloResponse;
import com.uniquindio.backend.application.usecase.AsignarRegaloPorMetaUseCase;
import com.uniquindio.backend.application.usecase.ConfirmarEntregaRegaloUseCase;
import com.uniquindio.backend.application.usecase.EnviarRegaloUseCase;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.rest.mapper.RegaloMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * El {@code usuarioId} sale del token JWT (Authentication): el regalo se asigna al usuario
 * autenticado y solo él puede confirmar su entrega.
 * Limitación: enviar un regalo (CU-24) es una operación de admin/sistema y aún no tiene
 * restricción por rol.
 */
@PreAuthorize("hasRole('COMPRADOR')")
@RestController
@RequestMapping("/api/regalos")
public class RegaloController {

    private final AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase;
    private final EnviarRegaloUseCase enviarRegaloUseCase;
    private final ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase;
    private final RegaloMapper mapper;

    public RegaloController(AsignarRegaloPorMetaUseCase asignarRegaloPorMetaUseCase,
                            EnviarRegaloUseCase enviarRegaloUseCase,
                            ConfirmarEntregaRegaloUseCase confirmarEntregaRegaloUseCase,
                            RegaloMapper mapper) {
        this.asignarRegaloPorMetaUseCase = asignarRegaloPorMetaUseCase;
        this.enviarRegaloUseCase = enviarRegaloUseCase;
        this.confirmarEntregaRegaloUseCase = confirmarEntregaRegaloUseCase;
        this.mapper = mapper;
    }

    // CU-10 Asignar Regalo por Meta al usuario autenticado (5 compras COMPLETADAS por regalo)
    @PutMapping("/{id}/asignar")
    public ResponseEntity<RegaloResponse> asignar(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        Regalo regalo = asignarRegaloPorMetaUseCase.ejecutar(usuarioId, id);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }

    // CU-24 Enviar Regalo (solo si ya fue elegido)
    @PreAuthorize("isAuthenticated()")
    @PutMapping("/{id}/enviar")
    public ResponseEntity<RegaloResponse> enviar(@PathVariable UUID id) {
        Regalo regalo = enviarRegaloUseCase.ejecutar(id);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }

    // CU-24 Confirmar Entrega (solo el destinatario autenticado, y solo si fue enviado)
    @PutMapping("/{id}/entrega")
    public ResponseEntity<RegaloResponse> confirmarEntrega(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        Regalo regalo = confirmarEntregaRegaloUseCase.ejecutar(id, usuarioId);
        return ResponseEntity.ok(mapper.toResponse(regalo)); // 200 OK
    }
}