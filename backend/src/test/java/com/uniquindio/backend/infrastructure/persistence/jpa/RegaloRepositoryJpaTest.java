package com.uniquindio.backend.infrastructure.persistence.jpa;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.repository.RegaloRepository;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.jpa.mapper.RegaloPersistenceMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void obtenerPorEstadoDevuelveSoloLosRegalosEnEseEstado() {
        Regalo disponible = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        Regalo elegido = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        elegido.elegir(UUID.randomUUID(), false);
        regaloRepository.guardar(disponible);
        regaloRepository.guardar(elegido);
        entityManager.flush();
        entityManager.clear();

        List<Regalo> disponibles = regaloRepository.obtenerPorEstado(EstadoRegalo.DISPONIBLE);

        assertEquals(1, disponibles.size());
        assertEquals(disponible.getId(), disponibles.get(0).getId());
    }

    @Test
    void obtenerPorUsuarioIdDevuelveSoloLosRegalosDeEseUsuario() {
        UUID usuario = UUID.randomUUID();
        Regalo suyo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        suyo.elegir(usuario, false);
        Regalo deOtro = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        deOtro.elegir(UUID.randomUUID(), false);
        regaloRepository.guardar(suyo);
        regaloRepository.guardar(deOtro);
        entityManager.flush();
        entityManager.clear();

        List<Regalo> mios = regaloRepository.obtenerPorUsuarioId(usuario);

        assertEquals(1, mios.size());
        assertEquals(suyo.getId(), mios.get(0).getId());
        assertTrue(regaloRepository.obtenerPorUsuarioId(UUID.randomUUID()).isEmpty());
    }
}
