package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.AsistenteChatRequest;
import com.uniquindio.backend.application.dto.request.MensajeHistorialRequest;
import com.uniquindio.backend.application.dto.response.AsistenteChatResponse;
import com.uniquindio.backend.application.port.out.MensajeIA;
import com.uniquindio.backend.application.usecase.ChatAsistenteCompradorUseCase;
import com.uniquindio.backend.application.usecase.ChatAsistenteVendedorUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Asistente de IA: un endpoint por rol. El id de la persona sale siempre del token (JWT),
 * nunca del cuerpo de la petición. Los fallos del proveedor llegan como ResponseStatusException
 * (429, 502, 503, 504) y los traduce el GlobalExceptionHandler.
 */
@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteController {

    private final ChatAsistenteCompradorUseCase chatAsistenteCompradorUseCase;
    private final ChatAsistenteVendedorUseCase chatAsistenteVendedorUseCase;

    // Recomendaciones de productos y combos para el comprador autenticado
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/comprador")
    public ResponseEntity<AsistenteChatResponse> comprador(@AuthenticationPrincipal UUID usuarioId,
                                                           @Valid @RequestBody AsistenteChatRequest request) {
        String respuesta = chatAsistenteCompradorUseCase.ejecutar(
                usuarioId, request.mensaje(), aHistorial(request.historial()));
        return ResponseEntity.ok(new AsistenteChatResponse(respuesta)); // 200 OK
    }

    // Análisis del dashboard para el vendedor autenticado
    @PreAuthorize("hasRole('VENDEDOR')")
    @PostMapping("/vendedor")
    public ResponseEntity<AsistenteChatResponse> vendedor(@AuthenticationPrincipal UUID vendedorId,
                                                          @Valid @RequestBody AsistenteChatRequest request) {
        String respuesta = chatAsistenteVendedorUseCase.ejecutar(
                vendedorId, request.mensaje(), aHistorial(request.historial()));
        return ResponseEntity.ok(new AsistenteChatResponse(respuesta)); // 200 OK
    }

    private static List<MensajeIA> aHistorial(List<MensajeHistorialRequest> historial) {
        if (historial == null) {
            return List.of();
        }
        return historial.stream()
                .map(m -> new MensajeIA(
                        "user".equals(m.rol()) ? MensajeIA.Rol.USUARIO : MensajeIA.Rol.ASISTENTE,
                        m.contenido()))
                .toList();
    }
}
