package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.repository.CuponRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.CuponPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({CuponRepositoryJpa.class, CuponPersistenceMapper.class})
class CuponRepositoryJpaTest {

    private static final LocalDate VENCE = LocalDate.of(2026, 12, 31);

    @Autowired
    private CuponRepository cuponRepository;
    @Autowired
    private EntityManager entityManager;

    private Cupon guardar(UUID vendedorId, String codigo) {
        Cupon cupon = Cupon.crear(UUID.randomUUID(), vendedorId, codigo, 10, VENCE, 5);
        cuponRepository.guardar(cupon);
        return cupon;
    }

    private void sincronizar() {
        entityManager.flush();
        entityManager.clear();
    }

    private Set<String> codigos(List<Cupon> cupones) {
        return cupones.stream().map(Cupon::getCodigo).collect(Collectors.toSet());
    }

    @Test
    void obtenerPorVendedorIdDevuelveSoloLosCuponesDeEseVendedor() {
        UUID vendedorA = UUID.randomUUID();
        UUID vendedorB = UUID.randomUUID();
        guardar(vendedorA, "A1");
        guardar(vendedorA, "A2");
        guardar(vendedorB, "B1");
        guardar(null, "GLOBAL");   // global: no pertenece a ningún vendedor
        sincronizar();

        assertEquals(Set.of("A1", "A2"), codigos(cuponRepository.obtenerPorVendedorId(vendedorA)));
        assertEquals(Set.of("B1"), codigos(cuponRepository.obtenerPorVendedorId(vendedorB)));
        assertTrue(cuponRepository.obtenerPorVendedorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void existePorCodigoNormalizaMayusculasYEspacios() {
        guardar(UUID.randomUUID(), "VERANO30");
        sincronizar();

        assertTrue(cuponRepository.existePorCodigo("verano30"));
        assertTrue(cuponRepository.existePorCodigo("  VERANO30 "));
        assertFalse(cuponRepository.existePorCodigo("OTRO"));
        assertFalse(cuponRepository.existePorCodigo(null));
    }

    @Test
    void registrarUsoDeMarcaSoloAEseCompradorYEsIdempotente() {
        Cupon cupon = guardar(null, "UNAVEZ");
        UUID ana = UUID.randomUUID();
        UUID luis = UUID.randomUUID();
        sincronizar();
        assertFalse(cuponRepository.fueUsadoPor(cupon.getId(), ana));

        cuponRepository.registrarUsoDe(cupon.getId(), ana);
        cuponRepository.registrarUsoDe(cupon.getId(), ana);   // repetirlo no duplica ni rompe la restricción
        sincronizar();

        assertTrue(cuponRepository.fueUsadoPor(cupon.getId(), ana));
        assertFalse(cuponRepository.fueUsadoPor(cupon.getId(), luis));
        assertFalse(cuponRepository.fueUsadoPor(UUID.randomUUID(), ana));
    }

    @Test
    void conservaElDuenoYLosUsosAlLeer() {
        UUID vendedorId = UUID.randomUUID();
        Cupon propio = guardar(vendedorId, "PROPIO");
        Cupon global = guardar(null, "GLOBAL");
        propio.registrarUso();
        cuponRepository.guardar(propio);
        sincronizar();

        Cupon leidoPropio = cuponRepository.obtenerPorCodigo("propio").orElseThrow();
        Cupon leidoGlobal = cuponRepository.obtenerPorCodigo("GLOBAL").orElseThrow();

        assertEquals(vendedorId, leidoPropio.getVendedorId());
        assertEquals(1, leidoPropio.getUsos());
        assertNull(leidoGlobal.getVendedorId());
        assertEquals(global.getId(), leidoGlobal.getId());
    }
}
