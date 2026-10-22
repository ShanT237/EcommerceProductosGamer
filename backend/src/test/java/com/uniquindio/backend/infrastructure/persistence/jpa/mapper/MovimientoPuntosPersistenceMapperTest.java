package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.MovimientoPuntos;
import com.uniquindio.backend.domain.valueobject.TipoMovimientoPuntos;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MovimientoPuntosEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovimientoPuntosPersistenceMapperTest {

    private final MovimientoPuntosPersistenceMapper mapper = new MovimientoPuntosPersistenceMapper();

    @Test
    @DisplayName("MovimientoPuntos ganado: toEntity → toDomain conserva id, tipo y cantidad")
    void ganadoIdaYVuelta() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 19, 12, 30);
        MovimientoPuntos original = MovimientoPuntos.ganado(UUID.randomUUID(), 30, fecha);

        MovimientoPuntosEntity entity = mapper.toEntity(original);
        MovimientoPuntos reconstruido = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstruido.getId());
        assertEquals(original.getUsuarioId(), reconstruido.getUsuarioId());
        assertEquals(30, reconstruido.getCantidad());
        assertEquals(TipoMovimientoPuntos.GANADO, reconstruido.getTipo());
        assertEquals(fecha, reconstruido.getFecha());
    }

    @Test
    @DisplayName("MovimientoPuntos redimido: toEntity → toDomain conserva el tipo REDIMIDO")
    void redimidoIdaYVuelta() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 19, 13, 0);
        MovimientoPuntos original = MovimientoPuntos.redimido(UUID.randomUUID(), 10, fecha);

        MovimientoPuntos reconstruido = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original.getId(), reconstruido.getId());
        assertEquals(10, reconstruido.getCantidad());
        assertEquals(TipoMovimientoPuntos.REDIMIDO, reconstruido.getTipo());
    }
}