package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.CompraPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
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
@Import({CompraRepositoryJpa.class, CompraPersistenceMapper.class})
class CompraRepositoryJpaTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);

    @Autowired
    private CompraRepository compraRepository;
    @Autowired
    private EntityManager entityManager;

    private Compra guardar(UUID usuarioId, UUID pedidoId) {
        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 2, AHORA,
                new Precio(100), pedidoId);
        compraRepository.guardar(compra);
        return compra;
    }

    private Set<UUID> ids(List<Compra> compras) {
        return compras.stream().map(Compra::getId).collect(Collectors.toSet());
    }

    @Test
    void obtenerPorPedidoIdDevuelveSoloLasLineasDelPedido() {
        UUID usuarioId = UUID.randomUUID();
        UUID pedidoA = UUID.randomUUID();
        UUID pedidoB = UUID.randomUUID();
        Compra a1 = guardar(usuarioId, pedidoA);
        Compra a2 = guardar(usuarioId, pedidoA);
        Compra b1 = guardar(usuarioId, pedidoB);
        guardar(usuarioId, null); // compra suelta: no pertenece a ningún pedido
        entityManager.flush();
        entityManager.clear();

        assertEquals(Set.of(a1.getId(), a2.getId()), ids(compraRepository.obtenerPorPedidoId(pedidoA)));
        assertEquals(Set.of(b1.getId()), ids(compraRepository.obtenerPorPedidoId(pedidoB)));
    }

    @Test
    void pedidoInexistenteDevuelveListaVacia() {
        guardar(UUID.randomUUID(), UUID.randomUUID());
        entityManager.flush();
        entityManager.clear();

        assertTrue(compraRepository.obtenerPorPedidoId(UUID.randomUUID()).isEmpty());
    }

    @Test
    void conservaElPedidoIdAlLeerYLaCompraSueltaLoTieneNulo() {
        UUID pedidoId = UUID.randomUUID();
        Compra deUnPedido = guardar(UUID.randomUUID(), pedidoId);
        Compra suelta = guardar(UUID.randomUUID(), null);
        entityManager.flush();
        entityManager.clear();

        Compra leidaDelPedido = compraRepository.obtenerPorId(deUnPedido.getId()).orElseThrow();
        Compra leidaSuelta = compraRepository.obtenerPorId(suelta.getId()).orElseThrow();

        assertEquals(pedidoId, leidaDelPedido.getPedidoId());
        assertTrue(leidaDelPedido.perteneceAUnPedido());
        assertNull(leidaSuelta.getPedidoId());
        assertFalse(leidaSuelta.perteneceAUnPedido());
    }

    @Test
    void conservaElIdYNombreDelComboAlLeerLaCompra() {
        UUID usuarioId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        UUID comboId = UUID.randomUUID();
        Compra combo = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1, AHORA,
                new Precio(100), pedidoId, comboId, "Combo Gamer");
        compraRepository.guardar(combo);
        entityManager.flush();
        entityManager.clear();

        Compra leida = compraRepository.obtenerPorId(combo.getId()).orElseThrow();

        assertEquals(pedidoId, leida.getPedidoId());
        assertEquals(comboId, leida.getComboId());
        assertEquals("Combo Gamer", leida.getComboNombre());
    }

    @Test
    void laConfirmacionDeLasLineasQuedaPersistida() {
        UUID usuarioId = UUID.randomUUID();
        UUID pedidoId = UUID.randomUUID();
        guardar(usuarioId, pedidoId);
        guardar(usuarioId, pedidoId);
        entityManager.flush();
        entityManager.clear();

        List<Compra> lineas = compraRepository.obtenerPorPedidoId(pedidoId);
        lineas.forEach(Compra::confirmar);
        lineas.forEach(compraRepository::guardar);
        entityManager.flush();
        entityManager.clear();

        assertTrue(compraRepository.obtenerPorPedidoId(pedidoId).stream()
                .allMatch(c -> c.getEstado() == EstadoCompra.COMPLETADA));
    }

    @Test
    void obtenerPendientesAnterioresASoloDevuelvePendientesMasViejasQueElLimite() {
        LocalDateTime limite = AHORA.minusMinutes(10);
        Compra vieja = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AHORA.minusMinutes(30), new Precio(100));
        Compra nueva = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AHORA, new Precio(100));
        Compra justa = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                limite, new Precio(100));
        Compra viejaCompletada = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                AHORA.minusMinutes(30), new Precio(100));
        viejaCompletada.confirmar();
        compraRepository.guardar(vieja);
        compraRepository.guardar(nueva);
        compraRepository.guardar(justa);
        compraRepository.guardar(viejaCompletada);
        entityManager.flush();
        entityManager.clear();

        assertEquals(Set.of(vieja.getId()), ids(compraRepository.obtenerPendientesAnterioresA(limite)));
    }
}
