package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.VendedorPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({VendedorRepositoryJpa.class, VendedorPersistenceMapper.class})
class VendedorRepositoryJpaTest {

    @Autowired
    private VendedorRepository vendedorRepository;
    @Autowired
    private EntityManager entityManager;

    private Vendedor vendedor(String correo, String documento) {
        return Vendedor.registrar(UUID.randomUUID(), "GamerZone", correo,
                TipoVendedor.EMPRESA, documento);
    }

    @Test
    @DisplayName("obtenerPorCorreo y existeConCorreo ignoran mayúsculas")
    void buscaPorCorreoSinDistinguirMayusculas() {
        Vendedor v = vendedor("ventas@gamerzone.com", "900111");
        vendedorRepository.guardar(v);
        entityManager.flush();
        entityManager.clear();

        assertEquals(v.getId(), vendedorRepository.obtenerPorCorreo("VENTAS@gamerzone.com").orElseThrow().getId());
        assertTrue(vendedorRepository.existeConCorreo("Ventas@GamerZone.com"));
        assertTrue(vendedorRepository.obtenerPorCorreo("nadie@gamerzone.com").isEmpty());
        assertFalse(vendedorRepository.existeConCorreo(null));
    }

    @Test
    @DisplayName("La base rechaza dos vendedores con el mismo correo")
    void correoUnico() {
        vendedorRepository.guardar(vendedor("ventas@gamerzone.com", "900111"));
        entityManager.flush();

        assertThrows(RuntimeException.class, () -> {
            vendedorRepository.guardar(vendedor("ventas@gamerzone.com", "900222"));
            entityManager.flush();
        });
    }
}