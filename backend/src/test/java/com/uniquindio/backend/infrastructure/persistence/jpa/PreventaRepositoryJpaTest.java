package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.repository.PreventaRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.PreventaPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({PreventaRepositoryJpa.class, PreventaPersistenceMapper.class})
class PreventaRepositoryJpaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    @Autowired
    private PreventaRepository preventaRepository;
    @Autowired
    private EntityManager entityManager;

    private Preventa guardar(LocalDate fecha) {
        Preventa preventa = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(), fecha, "Lanzamiento");
        preventaRepository.guardar(preventa);
        return preventa;
    }

    @Test
    void obtenerFuturasExcluyeHoyYLasFechasPasadas() {
        Preventa futura = guardar(HOY.plusDays(3));
        guardar(HOY);
        guardar(HOY.minusDays(1));
        entityManager.flush();
        entityManager.clear();

        List<Preventa> resultado = preventaRepository.obtenerFuturas(HOY);

        assertEquals(List.of(futura.getId()), resultado.stream().map(Preventa::getId).toList());
    }

    @Test
    void noDevuelveResultadosCuandoNoHayLanzamientosFuturos() {
        guardar(HOY);
        guardar(HOY.minusDays(1));
        entityManager.flush();
        entityManager.clear();

        assertEquals(List.of(), preventaRepository.obtenerFuturas(HOY));
    }
}
