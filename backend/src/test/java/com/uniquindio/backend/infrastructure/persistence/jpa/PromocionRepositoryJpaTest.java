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
    @Test
    void obtenerPorIdDevuelveLaPromocionGuardada() {
        Promocion p = guardar(UUID.randomUUID(), HOY, HOY.plusDays(3));
        entityManager.flush();
        entityManager.clear();

        Promocion leida = promocionRepository.obtenerPorId(p.getId()).orElseThrow();

        assertEquals(p.getProductoId(), leida.getProductoId());
        assertEquals(15, leida.getPorcentaje());
        assertEquals(HOY, leida.getDesde());
        assertEquals(HOY.plusDays(3), leida.getHasta());
    }

    @Test
    void obtenerPorIdDeUnaInexistenteDevuelveVacio() {
        assertTrue(promocionRepository.obtenerPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void guardarDosVecesLaMismaPromocionLaActualizaSinDuplicar() {
        UUID productoId = UUID.randomUUID();
        Promocion p = guardar(productoId, HOY, HOY.plusDays(3));
        promocionRepository.guardar(p.reprogramar(40, HOY, HOY.plusDays(9)));
        entityManager.flush();
        entityManager.clear();

        List<Promocion> todas = promocionRepository.obtenerPorProductoId(productoId);

        assertEquals(1, todas.size());
        assertEquals(40, todas.get(0).getPorcentaje());
        assertEquals(HOY.plusDays(9), todas.get(0).getHasta());
    }

    @Test
    void eliminarBorraSoloLaPromocionIndicada() {
        UUID productoId = UUID.randomUUID();
        Promocion borrar = guardar(productoId, HOY, HOY.plusDays(3));
        Promocion conservar = guardar(productoId, HOY.plusDays(10), HOY.plusDays(15));
        entityManager.flush();
        entityManager.clear();

        promocionRepository.eliminar(borrar.getId());
        entityManager.flush();
        entityManager.clear();

        assertTrue(promocionRepository.obtenerPorId(borrar.getId()).isEmpty());
        assertTrue(promocionRepository.obtenerPorId(conservar.getId()).isPresent());
    }

    @Test
    void obtenerPorProductoIdsDevuelveTodasLasDeEsosProductosSinImportarLaVigencia() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID ajeno = UUID.randomUUID();
        Promocion vencida = guardar(a, HOY.minusDays(30), HOY.minusDays(20));
        Promocion futura = guardar(a, HOY.plusDays(10), HOY.plusDays(20));
        Promocion deB = guardar(b, HOY, HOY.plusDays(1));
        guardar(ajeno, HOY, HOY.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        List<Promocion> resultado = promocionRepository.obtenerPorProductoIds(List.of(a, b));

        assertEquals(3, resultado.size());
        assertTrue(resultado.stream().map(Promocion::getId).toList()
                .containsAll(List.of(vencida.getId(), futura.getId(), deB.getId())));
    }

    @Test
    void obtenerPorProductoIdsConListaVaciaDevuelveListaVacia() {
        assertTrue(promocionRepository.obtenerPorProductoIds(List.of()).isEmpty());
    }
}
