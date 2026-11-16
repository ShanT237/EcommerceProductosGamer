package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.response.NotificacionResponse;
import com.uniquindio.backend.application.usecase.ConsultarNotificacionesUseCase;
import com.uniquindio.backend.application.usecase.MarcarNotificacionLeidaUseCase;
import com.uniquindio.backend.infrastructure.rest.mapper.NotificacionMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@PreAuthorize("hasAnyRole('COMPRADOR', 'VENDEDOR')")
@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ConsultarNotificacionesUseCase consultarNotificacionesUseCase;
    private final MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase;
    private final NotificacionMapper mapper;

    public NotificacionController(ConsultarNotificacionesUseCase consultarNotificacionesUseCase,
                                  MarcarNotificacionLeidaUseCase marcarNotificacionLeidaUseCase,
                                  NotificacionMapper mapper) {
        this.consultarNotificacionesUseCase = consultarNotificacionesUseCase;
        this.marcarNotificacionLeidaUseCase = marcarNotificacionLeidaUseCase;
        this.mapper = mapper;
    }

    // Listar las notificaciones del usuario autenticado (más reciente primero)
    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(@AuthenticationPrincipal UUID usuarioId) {
        return ResponseEntity.ok(mapper.toResponses(consultarNotificacionesUseCase.ejecutar(usuarioId)));
    }

    // Marcar una notificación propia como leída
    @PutMapping("/{id}/leida")
    public ResponseEntity<NotificacionResponse> marcarLeida(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID usuarioId) {
        return ResponseEntity.ok(mapper.toResponse(marcarNotificacionLeidaUseCase.ejecutar(id, usuarioId)));
    }
}
