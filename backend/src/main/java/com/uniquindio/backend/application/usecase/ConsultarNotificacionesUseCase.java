package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Caso de uso: ConsultarNotificaciones (Regla 8).
 * Devuelve las notificaciones del usuario, la más reciente primero.
 */
@Service
@RequiredArgsConstructor
public class ConsultarNotificacionesUseCase {

    private final NotificacionRepository notificacionRepository;

    public List<Notificacion> ejecutar(UUID usuarioId) {
        return notificacionRepository.obtenerPorUsuarioId(usuarioId).stream()
                .sorted(Comparator.comparing(Notificacion::getFecha).reversed())
                .toList();
    }
}
