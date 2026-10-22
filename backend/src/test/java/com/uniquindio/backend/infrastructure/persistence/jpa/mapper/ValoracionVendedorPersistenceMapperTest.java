package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.ValoracionVendedor;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.ValoracionVendedorEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValoracionVendedorPersistenceMapperTest {

    private final ValoracionVendedorPersistenceMapper mapper = new ValoracionVendedorPersistenceMapper();

    @Test
    @DisplayName("ValoracionVendedor: toEntity → toDomain conserva todos los campos")
    void idaYVuelta() {
        LocalDateTime fecha = LocalDateTime.of(2026, 10, 19, 12, 0);
        ValoracionVendedor original = ValoracionVendedor.crear(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                true, new Calificacion(5), "Muy buen vendedor", fecha);

        ValoracionVendedorEntity entity = mapper.toEntity(original);
        ValoracionVendedor reconstruida = mapper.toDomain(entity);

        assertEquals(original.getId(), reconstruida.getId());
        assertEquals(original.getCompraId(), reconstruida.getCompraId());
        assertEquals(original.getUsuarioId(), reconstruida.getUsuarioId());
        assertEquals(original.getVendedorId(), reconstruida.getVendedorId());
        assertEquals(5, reconstruida.getCalificacion().estrellas());
        assertEquals("Muy buen vendedor", reconstruida.getComentario());
        assertEquals(fecha, reconstruida.getFecha());
    }
}