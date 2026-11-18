package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.ConversacionChat;
import com.uniquindio.backend.domain.entity.MensajeChat;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.ConversacionChatRepository;
import com.uniquindio.backend.domain.repository.MensajeChatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Historial paginado por cursor (no por número de página: llegan mensajes nuevos mientras se pagina).
 * Siempre entrega los mensajes en orden cronológico ascendente.
 */
@Service
@RequiredArgsConstructor
public class ConsultarHistorialChatUseCase {

    static final int TAMANO_MINIMO = 1;
    static final int TAMANO_MAXIMO = 100;

    private final ConversacionChatRepository conversacionRepository;
    private final MensajeChatRepository mensajeRepository;

    @Transactional(readOnly = true)
    public HistorialChat ejecutar(UUID usuarioId, UUID conversacionId, UUID antesDe, UUID despuesDe, int tamano) {
        ConversacionChat conversacion = conversacionRepository.obtenerPorId(conversacionId)
                .orElseThrow(() -> new NoSuchElementException("La conversación no existe"));
        conversacion.asegurarParticipante(usuarioId);

        if (antesDe != null && despuesDe != null) {
            throw new ReglaDominioException("Indica solo uno: antesDe o despuesDe");
        }
        int limite = Math.max(TAMANO_MINIMO, Math.min(TAMANO_MAXIMO, tamano));

        if (despuesDe != null) {
            MensajeChat cursor = cursorDe(despuesDe, conversacionId);
            List<MensajeChat> posteriores = new ArrayList<>(
                    mensajeRepository.obtenerPosteriores(conversacionId, cursor, limite + 1));
            boolean hayMas = posteriores.size() > limite;
            if (hayMas) {
                posteriores = new ArrayList<>(posteriores.subList(0, limite));
            }
            return new HistorialChat(posteriores, hayMas);
        }

        MensajeChat cursor = antesDe == null ? null : cursorDe(antesDe, conversacionId);
        List<MensajeChat> anteriores = new ArrayList<>(
                mensajeRepository.obtenerAnteriores(conversacionId, cursor, limite + 1));
        boolean hayMas = anteriores.size() > limite;
        if (hayMas) {
            anteriores = new ArrayList<>(anteriores.subList(0, limite));
        }
        Collections.reverse(anteriores); // vienen del más nuevo al más antiguo
        return new HistorialChat(anteriores, hayMas);
    }

    private MensajeChat cursorDe(UUID mensajeId, UUID conversacionId) {
        MensajeChat cursor = mensajeRepository.obtenerPorId(mensajeId)
                .orElseThrow(() -> new ReglaDominioException("El mensaje de referencia no existe"));
        if (!cursor.getConversacionId().equals(conversacionId)) {
            throw new ReglaDominioException("El mensaje de referencia no pertenece a la conversación");
        }
        return cursor;
    }
}
