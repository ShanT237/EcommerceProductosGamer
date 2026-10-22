package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.PromocionEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromocionPersistenceMapperTest {

    private final PromocionPersistenceMapper mapper = new PromocionPersistenceMapper();

    @Test
    @DisplayName("Promocion: toEntity → toDomain conserva producto, porcentaje y vigencia")
    void idaYVuelta() {
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        Promocion original = Promocion.programar(UUID.randomUUID(), UUID.randomUUID(), 20, desde, hasta);

        PromocionEntity entity = mapper.toEntity(original);
        Promocion reconstruida = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstruida.getId());
        assertEquals(original.getProductoId(), reconstruida.getProductoId());
        assertEquals(20, reconstruida.getPorcentaje());
        assertEquals(desde, reconstruida.getDesde());
        assertEquals(hasta, reconstruida.getHasta());
        assertTrue(reconstruida.estaVigente(LocalDate.of(2026, 11, 15)));
    }
}