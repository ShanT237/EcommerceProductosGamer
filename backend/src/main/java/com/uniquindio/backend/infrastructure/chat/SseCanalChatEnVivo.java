package com.uniquindio.backend.infrastructure.chat;

import com.uniquindio.backend.application.dto.response.LecturaChatResponse;
import com.uniquindio.backend.application.dto.response.MensajeChatResponse;
import com.uniquindio.backend.application.port.out.CanalChatEnVivo;
import com.uniquindio.backend.application.port.out.EventoChat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Canal en vivo del chat sobre SSE: un registro en memoria de emisores por usuario.
 * Límite aceptado: válido para una sola instancia del backend (escalar exigiría un broker).
 */
@Component
public class SseCanalChatEnVivo implements CanalChatEnVivo {

    static final int MAX_CONEXIONES_POR_USUARIO = 5;
    private static final long REINTENTO_CLIENTE_MS = 3000;
    private static final Logger log = LoggerFactory.getLogger(SseCanalChatEnVivo.class);

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emisores = new ConcurrentHashMap<>();
    private final long timeoutMs;

    public SseCanalChatEnVivo(@Value("${chat.sse.timeout-ms}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /** Abre un stream para el usuario; si ya tiene 5 abiertos se cierra el más antiguo. */
    public SseEmitter suscribir(UUID usuarioId) {
        SseEmitter emisor = new SseEmitter(timeoutMs);
        List<SseEmitter> desalojados = new ArrayList<>();

        emisores.compute(usuarioId, (id, lista) -> {
            CopyOnWriteArrayList<SseEmitter> actuales = lista != null ? lista : new CopyOnWriteArrayList<>();
            while (actuales.size() >= MAX_CONEXIONES_POR_USUARIO) {
                desalojados.add(actuales.remove(0));
            }
            actuales.add(emisor);
            return actuales;
        });
        // Se completan fuera de compute(): sus callbacks vuelven a tocar el mapa.
        desalojados.forEach(this::completarSilencioso);

        emisor.onCompletion(() -> retirar(usuarioId, emisor));
        emisor.onTimeout(() -> retirar(usuarioId, emisor));
        emisor.onError(e -> retirar(usuarioId, emisor));

        // Evento inicial: fuerza a que las cabeceras salgan ya y confirma la conexión al cliente.
        try {
            synchronized (emisor) {
                emisor.send(SseEmitter.event().name("conectado").reconnectTime(REINTENTO_CLIENTE_MS).data("ok"));
            }
        } catch (IOException | IllegalStateException e) {
            retirar(usuarioId, emisor);
        }
        return emisor;
    }

    @Override
    public void publicar(Collection<UUID> destinatarios, EventoChat evento) {
        List<UUID> unicos = new ArrayList<>(new LinkedHashSet<>(destinatarios));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enviar(unicos, evento);
                }
            });
        } else {
            enviar(unicos, evento);
        }
    }

    @Scheduled(fixedRateString = "${chat.sse.heartbeat-ms:25000}")
    void latido() {
        emisores.forEach((usuarioId, lista) -> {
            for (SseEmitter emisor : lista) {
                enviarA(usuarioId, emisor, SseEmitter.event().comment("ping"));
            }
        });
    }

    private void enviar(List<UUID> destinatarios, EventoChat evento) {
        for (UUID usuarioId : destinatarios) {
            List<SseEmitter> lista = emisores.get(usuarioId);
            if (lista == null) {
                continue;
            }
            for (SseEmitter emisor : lista) {
                enviarA(usuarioId, emisor, construirEvento(evento));
            }
        }
    }

    private SseEmitter.SseEventBuilder construirEvento(EventoChat evento) {
        return switch (evento) {
            case EventoChat.MensajeNuevo e -> SseEmitter.event()
                    .name("mensaje")
                    .data(MensajeChatResponse.desde(e.mensaje()), MediaType.APPLICATION_JSON);
            case EventoChat.MensajesLeidos e -> SseEmitter.event()
                    .name("leidos")
                    .data(new LecturaChatResponse(e.conversacionId(), e.lectorId(), e.leidoEn()),
                            MediaType.APPLICATION_JSON);
        };
    }

    private void enviarA(UUID usuarioId, SseEmitter emisor, SseEmitter.SseEventBuilder evento) {
        try {
            // SseEmitter.send no es seguro entre hilos (latido, publicación y suscripción a la vez).
            synchronized (emisor) {
                emisor.send(evento);
            }
        } catch (IOException | IllegalStateException e) {
            log.debug("Emisor SSE retirado tras un error de envío: {}", e.getMessage());
            retirar(usuarioId, emisor);
        }
    }

    private void retirar(UUID usuarioId, SseEmitter emisor) {
        emisores.computeIfPresent(usuarioId, (id, lista) -> {
            lista.remove(emisor);
            return lista.isEmpty() ? null : lista;
        });
    }

    private void completarSilencioso(SseEmitter emisor) {
        try {
            synchronized (emisor) {
                emisor.complete();
            }
        } catch (RuntimeException ignorada) {
            // ya estaba cerrado
        }
    }
}
