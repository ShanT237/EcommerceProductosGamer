package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.application.dto.request.EnviarMensajeChatRequest;
import com.uniquindio.backend.application.dto.request.IniciarConversacionChatRequest;
import com.uniquindio.backend.application.dto.response.ConversacionChatResponse;
import com.uniquindio.backend.application.dto.response.HistorialChatResponse;
import com.uniquindio.backend.application.dto.response.MarcadosLeidosResponse;
import com.uniquindio.backend.application.dto.response.MensajeChatResponse;
import com.uniquindio.backend.application.usecase.ConsultarHistorialChatUseCase;
import com.uniquindio.backend.application.usecase.EnviarMensajeChatUseCase;
import com.uniquindio.backend.application.usecase.IniciarConversacionChatUseCase;
import com.uniquindio.backend.application.usecase.ListarConversacionesChatUseCase;
import com.uniquindio.backend.application.usecase.MarcarLeidosChatUseCase;
import com.uniquindio.backend.infrastructure.rest.mapper.ChatMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Chat comprador–vendedor. La identidad sale siempre del JWT, nunca del body ni de la URL. */
@PreAuthorize("hasAnyRole('COMPRADOR','VENDEDOR')")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final IniciarConversacionChatUseCase iniciarConversacionUseCase;
    private final ListarConversacionesChatUseCase listarConversacionesUseCase;
    private final EnviarMensajeChatUseCase enviarMensajeUseCase;
    private final ConsultarHistorialChatUseCase consultarHistorialUseCase;
    private final MarcarLeidosChatUseCase marcarLeidosUseCase;
    private final ChatMapper mapper;

    // Solo el comprador inicia; es idempotente (devuelve la conversación existente del contexto)
    @PreAuthorize("hasRole('COMPRADOR')")
    @PostMapping("/conversaciones")
    public ResponseEntity<ConversacionChatResponse> iniciar(
            @AuthenticationPrincipal UUID usuarioId,
            @Valid @RequestBody IniciarConversacionChatRequest request) {
        return ResponseEntity.ok(mapper.toResponse(iniciarConversacionUseCase.ejecutar(
                usuarioId, request.tipo(), request.referenciaId(), request.vendedorId())));
    }

    @GetMapping("/conversaciones")
    public ResponseEntity<List<ConversacionChatResponse>> listar(@AuthenticationPrincipal UUID usuarioId) {
        return ResponseEntity.ok(mapper.toResponses(listarConversacionesUseCase.ejecutar(usuarioId)));
    }

    // Historial por cursor: antesDe pagina hacia atrás, despuesDe hace el catch-up hacia adelante
    @GetMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<HistorialChatResponse> historial(
            @AuthenticationPrincipal UUID usuarioId,
            @PathVariable UUID id,
            @RequestParam(required = false) UUID antesDe,
            @RequestParam(required = false) UUID despuesDe,
            @RequestParam(defaultValue = "30") int tamano) {
        return ResponseEntity.ok(mapper.toResponse(
                consultarHistorialUseCase.ejecutar(usuarioId, id, antesDe, despuesDe, tamano)));
    }

    // El remitente es el usuario del JWT; reenviar el mismo clienteId no duplica el mensaje
    @PostMapping("/conversaciones/{id}/mensajes")
    public ResponseEntity<MensajeChatResponse> enviar(
            @AuthenticationPrincipal UUID usuarioId,
            @PathVariable UUID id,
            @Valid @RequestBody EnviarMensajeChatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(
                enviarMensajeUseCase.ejecutar(usuarioId, id, request.clienteId(), request.contenido())));
    }

    @PutMapping("/conversaciones/{id}/leidos")
    public ResponseEntity<MarcadosLeidosResponse> marcarLeidos(
            @AuthenticationPrincipal UUID usuarioId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(mapper.toMarcados(marcarLeidosUseCase.ejecutar(usuarioId, id)));
    }
}
