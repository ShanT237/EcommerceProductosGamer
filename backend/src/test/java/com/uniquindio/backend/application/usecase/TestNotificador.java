package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.service.NotificadorEventos;
import com.uniquindio.backend.domain.repository.NotificacionRepository;
import com.uniquindio.backend.domain.repository.WishlistRepository;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;

/** Fábrica de {@link NotificadorEventos} con repositorios en memoria para los tests de casos de uso. */
final class TestNotificador {

    private TestNotificador() {
    }

    static NotificadorEventos nuevo() {
        return new NotificadorEventos(new NotificacionRepositoryEnMemoria(), new WishlistRepositoryEnMemoria());
    }

    static NotificadorEventos con(NotificacionRepository notificaciones, WishlistRepository wishlists) {
        return new NotificadorEventos(notificaciones, wishlists);
    }
}
