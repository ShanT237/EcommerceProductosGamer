package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.repository.ComboRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.ComboPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({ComboRepositoryJpa.class, ComboPersistenceMapper.class})
class ComboRepositoryJpaTest {

    @Autowired
    private ComboRepository comboRepository;
    @Autowired
    private EntityManager entityManager;

    private Combo guardar(UUID vendedorId, String nombre) {
        Combo combo = Combo.crear(UUID.randomUUID(), vendedorId, nombre, "Desc", 10,
                List.of(UUID.randomUUID(), UUID.randomUUID()));
        comboRepository.guardar(combo);
        return combo;
    }

    private Set<UUID> ids(List<Combo> combos) {
        return combos.stream().map(Combo::getId).collect(Collectors.toSet());
    }

    @Test
    void obtenerPorVendedorIdFiltraYObtenerTodosListaTodos() {
        UUID vendedorA = UUID.randomUUID();
        UUID vendedorB = UUID.randomUUID();
        Combo a1 = guardar(vendedorA, "A1");
        Combo a2 = guardar(vendedorA, "A2");
        Combo b1 = guardar(vendedorB, "B1");
        entityManager.flush();
        entityManager.clear();

        assertEquals(Set.of(a1.getId(), a2.getId()), ids(comboRepository.obtenerPorVendedorId(vendedorA)));
        assertEquals(Set.of(b1.getId()), ids(comboRepository.obtenerPorVendedorId(vendedorB)));
        assertEquals(Set.of(a1.getId(), a2.getId(), b1.getId()), ids(comboRepository.obtenerTodos()));
    }

    @Test
    void conservaLosProductosDelComboAlLeer() {
        Combo combo = guardar(UUID.randomUUID(), "Con productos");
        entityManager.flush();
        entityManager.clear();

        Combo leido = comboRepository.obtenerPorVendedorId(combo.getVendedorId()).get(0);

        assertEquals(Set.copyOf(combo.getProductos()), Set.copyOf(leido.getProductos()));
    }

    @Test
    void vendedorSinCombosDevuelveListaVacia() {
        assertTrue(comboRepository.obtenerPorVendedorId(UUID.randomUUID()).isEmpty());
    }
}
