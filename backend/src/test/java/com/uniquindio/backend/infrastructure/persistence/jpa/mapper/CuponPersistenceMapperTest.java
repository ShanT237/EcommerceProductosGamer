package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.CuponEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CuponPersistenceMapperTest {

    private final CuponPersistenceMapper mapper = new CuponPersistenceMapper();

    @Test
    @DisplayName("Cupon: toEntity → toDomain conserva código normalizado y usos")
    void idaYVuelta() {
        Cupon original = Cupon.crear(UUID.randomUUID(), " verano26 ", 15, LocalDate.of(2026, 12, 31), 3);
        original.registrarUso();

        CuponEntity entity = mapper.toEntity(original);
        Cupon reconstruido = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstruido.getId());
        assertEquals("VERANO26", reconstruido.getCodigo());
        assertEquals(15, reconstruido.getPorcentaje());
        assertEquals(LocalDate.of(2026, 12, 31), reconstruido.getVence());
        assertEquals(3, reconstruido.getUsosMaximos());
        assertEquals(1, reconstruido.getUsos());
        assertNull(reconstruido.getVendedorId());   // cupón global
    }

    @Test
    @DisplayName("Cupon con dueño: toEntity → toDomain conserva el vendedorId")
    void conservaElDuenoDelCupon() {
        UUID vendedorId = UUID.randomUUID();
        Cupon original = Cupon.crear(UUID.randomUUID(), vendedorId, "tienda10", 10, LocalDate.of(2026, 12, 31), 5);

        CuponEntity entity = mapper.toEntity(original);
        Cupon reconstruido = mapper.toDomain(entity);

        assertEquals(vendedorId, entity.getVendedorId());
        assertEquals(vendedorId, reconstruido.getVendedorId());
        assertEquals("TIENDA10", reconstruido.getCodigo());
    }
}
