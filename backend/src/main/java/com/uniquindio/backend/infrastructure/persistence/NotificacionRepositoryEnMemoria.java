package com.uniquindio.backend.infrastructure.persistence;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class NotificacionRepositoryEnMemoria implements NotificacionRepository {

    private final Map<UUID, Notificacion> storage = new ConcurrentHashMap<>();

    @Override
    public void guardar(Notificacion notificacion) {
        storage.put(notificacion.getId(), notificacion);
    }

    @Override
    public Optional<Notificacion> obtenerPorId(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Notificacion> obtenerPorUsuarioId(UUID usuarioId) {
        List<Notificacion> resultado = new ArrayList<>();
        for (Notificacion n : storage.values()) {
            if (n.getUsuarioId().equals(usuarioId)) {
                resultado.add(n);
            }
        }
        return resultado;
    }
}
