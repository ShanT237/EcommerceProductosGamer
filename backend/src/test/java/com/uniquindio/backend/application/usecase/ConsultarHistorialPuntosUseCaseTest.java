package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;
import com.uniquindio.backend.infrastructure.persistence.MovimientoPuntosRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarHistorialPuntosUseCaseTest {

    private MovimientoPuntosRepositoryEnMemoria repository;
    private ConsultarHistorialPuntosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new MovimientoPuntosRepositoryEnMemoria();
        useCase = new ConsultarHistorialPuntosUseCase(repository);
    }

    @Test
    void devuelveSoloLosMovimientosDelUsuarioMasRecientePrimero() {
        UUID usuarioId = UUID.randomUUID();
        MovimientoPuntos viejo = MovimientoPuntos.ganado(usuarioId, 10, LocalDateTime.of(2026, 9, 1, 10, 0));
        MovimientoPuntos reciente = MovimientoPuntos.redimido(usuarioId, 5, LocalDateTime.of(2026, 10, 1, 10, 0));
        MovimientoPuntos medio = MovimientoPuntos.ganado(usuarioId, 10, LocalDateTime.of(2026, 9, 15, 10, 0));
        repository.guardar(viejo);
        repository.guardar(reciente);
        repository.guardar(medio);
        repository.guardar(MovimientoPuntos.ganado(UUID.randomUUID(), 10, LocalDateTime.of(2026, 10, 5, 10, 0)));

        assertEquals(List.of(reciente, medio, viejo), useCase.ejecutar(usuarioId));
    }

    @Test
    void usuarioSinMovimientosDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }

    @Test
    void movimientoTieneTipoYRechazaCantidadNoPositiva() {
        UUID usuarioId = UUID.randomUUID();
        LocalDateTime fecha = LocalDateTime.of(2026, 9, 1, 10, 0);

        assertEquals(TipoMovimientoPuntos.GANADO, MovimientoPuntos.ganado(usuarioId, 10, fecha).getTipo());
        assertEquals(TipoMovimientoPuntos.REDIMIDO, MovimientoPuntos.redimido(usuarioId, 10, fecha).getTipo());
        assertThrows(ReglaDominioException.class, () -> MovimientoPuntos.ganado(usuarioId, 0, fecha));
        assertThrows(ReglaDominioException.class, () -> MovimientoPuntos.redimido(usuarioId, -1, fecha));
    }
}
