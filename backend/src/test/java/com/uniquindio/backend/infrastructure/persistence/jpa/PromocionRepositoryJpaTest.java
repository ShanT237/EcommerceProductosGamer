package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.repository.PromocionRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.PromocionPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({PromocionRepositoryJpa.class, PromocionPersistenceMapper.class})
class PromocionRepositoryJpaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    @Autowired
    private PromocionRepository promocionRepository;
    @Autowired
    private EntityManager entityManager;

    private Promocion guardar(UUID productoId, LocalDate desde, LocalDate hasta) {
        Promocion p = Promocion.programar(UUID.randomUUID(), productoId, 15, desde, hasta);
        promocionRepository.guardar(p);
        return p;
    }

    @Test
    void obtenerVigentesPorProductoIdsDevuelveSoloLasVigentesDeEsosProductos() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID vencido = UUID.randomUUID();
        UUID ajeno = UUID.randomUUID();
        Promocion deA = guardar(a, HOY.minusDays(1), HOY.plusDays(5));
        Promocion deB = guardar(b, HOY, HOY);                       // vigente solo hoy (límites inclusivos)
        guardar(vencido, HOY.minusDays(10), HOY.minusDays(1));
        guardar(ajeno, HOY.minusDays(1), HOY.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        Map<UUID, Promocion> resultado = promocionRepository.obtenerVigentesPorProductoIds(List.of(a, b, vencido), HOY);

        assertEquals(2, resultado.size());
        assertEquals(deA.getId(), resultado.get(a).getId());
        assertEquals(deB.getId(), resultado.get(b).getId());
    }

    @Test
    void conListaVaciaDevuelveUnMapaVacioSinConsultar() {
        assertTrue(promocionRepository.obtenerVigentesPorProductoIds(List.of(), HOY).isEmpty());
    }
}
