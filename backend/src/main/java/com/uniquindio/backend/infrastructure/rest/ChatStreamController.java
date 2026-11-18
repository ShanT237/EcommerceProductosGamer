package com.uniquindio.backend.infrastructure.rest;

import com.uniquindio.backend.infrastructure.chat.SseCanalChatEnVivo;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/** Stream SSE por usuario: el servidor empuja mensajes y confirmaciones de lectura. */
@PreAuthorize("hasAnyRole('COMPRADOR','VENDEDOR')")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatStreamController {

    private final SseCanalChatEnVivo canal;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter flujo(@AuthenticationPrincipal UUID usuarioId, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-cache");
        response.setHeader("X-Accel-Buffering", "no"); // evita que proxies (nginx) acumulen el stream
        return canal.suscribir(usuarioId);
    }
}
