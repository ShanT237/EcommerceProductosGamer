package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.MetaRecompensaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetaRecompensaPersistenceMapperTest {

    private final MetaRecompensaPersistenceMapper mapper = new MetaRecompensaPersistenceMapper();

    @Test
    @DisplayName("MetaRecompensa activa: toEntity → toDomain conserva umbral y producto regalo")
    void activaIdaYVuelta() {
        MetaRecompensa original = MetaRecompensa.crear(UUID.randomUUID(), 5, UUID.randomUUID());

        MetaRecompensaEntity entity = mapper.toEntity(original);
        MetaRecompensa reconstruida = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstruida.getId());
        assertEquals(5, reconstruida.getComprasRequeridas());
        assertEquals(original.getProductoRegaloId(), reconstruida.getProductoRegaloId());
        assertTrue(reconstruida.isActiva());
    }

    @Test
    @DisplayName("MetaRecompensa desactivada: toEntity → toDomain conserva activa = false")
    void desactivadaIdaYVuelta() {
        MetaRecompensa original = MetaRecompensa.crear(UUID.randomUUID(), 3, UUID.randomUUID());
        original.desactivar();

        MetaRecompensa reconstruida = mapper.toDomain(mapper.toEntity(original));

        assertFalse(reconstruida.isActiva());
    }
}