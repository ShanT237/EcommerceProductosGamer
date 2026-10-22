package com.uniquindio.backend.infrastructure.persistence.jpa.mapper;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.jpa.entity.VendedorEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VendedorPersistenceMapperTest {

    private final VendedorPersistenceMapper mapper = new VendedorPersistenceMapper();

    @Test
    @DisplayName("Vendedor: toEntity → toDomain conserva el hash de la contraseña")
    void idaYVueltaConservaElHash() {
        Vendedor original = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com",
                TipoVendedor.EMPRESA, "900123");
        original.definirContrasena("hash-de-prueba");

        VendedorEntity entity = mapper.toEntity(original);
        Vendedor reconstruido = mapper.toDomain(entity);

        assertEquals("hash-de-prueba", entity.getContrasenaHash());
        assertEquals("hash-de-prueba", reconstruido.obtenerHashContrasena());
        assertEquals(original.getId(), reconstruido.getId());
    }

    @Test
    @DisplayName("Vendedor sin contraseña: el hash sigue siendo null al ir y volver")
    void sinHashSigueNulo() {
        Vendedor original = Vendedor.registrar(UUID.randomUUID(), "Carlos", "c@c.com",
                TipoVendedor.PERSONA_NATURAL, "1094567890");

        assertNull(mapper.toDomain(mapper.toEntity(original)).obtenerHashContrasena());
    }
}