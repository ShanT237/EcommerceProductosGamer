package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * Caso de uso: MarcarNotificacionLeida (Regla 8).
 * Solo el dueño de la notificación puede marcarla como leída.
 */
@Service
@RequiredArgsConstructor
public class MarcarNotificacionLeidaUseCase {

    private final NotificacionRepository notificacionRepository;

    public Notificacion ejecutar(UUID notificacionId, UUID usuarioId) {
        Notificacion notificacion = notificacionRepository.obtenerPorId(notificacionId)
                .orElseThrow(() -> new NoSuchElementException("La notificación no existe"));

        notificacion.asegurarPerteneceA(usuarioId);
        notificacion.marcarComoLeida();
        notificacionRepository.guardar(notificacion);

        return notificacion;
    }
}
