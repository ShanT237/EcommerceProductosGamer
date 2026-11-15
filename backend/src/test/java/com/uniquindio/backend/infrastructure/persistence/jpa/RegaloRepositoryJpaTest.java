package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.RegaloPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
@Import({RegaloRepositoryJpa.class, RegaloPersistenceMapper.class})
class RegaloRepositoryJpaTest {

    @Autowired
    private RegaloRepository regaloRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void existePendienteDelComboSoloCuentaRegalosSinCerrarDeEseCombo() {
        UUID combo = UUID.randomUUID();
        UUID otroCombo = UUID.randomUUID();
        regaloRepository.guardar(Regalo.crear(UUID.randomUUID(), null, combo));
        entityManager.flush();
        entityManager.clear();

        assertTrue(regaloRepository.existePendienteDelCombo(combo));
        assertFalse(regaloRepository.existePendienteDelCombo(otroCombo));
    }

    @Test
    void unRegaloCanceladoNoCuentaComoPendiente() {
        UUID combo = UUID.randomUUID();
        Regalo regalo = Regalo.crear(UUID.randomUUID(), null, combo);
        regalo.cancelarRegalo();
        regaloRepository.guardar(regalo);
        entityManager.flush();
        entityManager.clear();

        assertFalse(regaloRepository.existePendienteDelCombo(combo));
    }
}
