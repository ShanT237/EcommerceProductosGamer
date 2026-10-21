package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EnviarYConfirmarRegaloUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private EnviarRegaloUseCase enviar;
    private ConfirmarEntregaRegaloUseCase confirmar;
    private UUID id;
    private UUID destinatarioId;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        enviar = new EnviarRegaloUseCase(regaloRepository);
        confirmar = new ConfirmarEntregaRegaloUseCase(regaloRepository);
        id = UUID.randomUUID();
        destinatarioId = UUID.randomUUID();
    }

    private void guardarRegaloElegido() {
        Regalo regalo = Regalo.crear(id, UUID.randomUUID(), null);
        regalo.elegir(destinatarioId, false);
        regaloRepository.guardar(regalo);
    }

    @Test
    void flujoElegidoEnviadoEntregado() {
        guardarRegaloElegido();

        assertEquals(EstadoRegalo.ENVIADO, enviar.ejecutar(id).getEstado());
        Regalo r = confirmar.ejecutar(id, destinatarioId);

        assertEquals(EstadoRegalo.ENTREGADO, r.getEstado());
        assertNotNull(r.getFechaEntrega());
        assertEquals(EstadoRegalo.ENTREGADO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void enviarRegaloDisponibleLanzaReglaDominio() {
        regaloRepository.guardar(Regalo.crear(id, UUID.randomUUID(), null));

        assertThrows(ReglaDominioException.class, () -> enviar.ejecutar(id));
        assertEquals(EstadoRegalo.DISPONIBLE, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void confirmarSinEnviarLanzaReglaDominio() {
        guardarRegaloElegido();

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(id, destinatarioId));
        assertEquals(EstadoRegalo.ELEGIDO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void confirmarPorUsuarioQueNoEsElDestinatarioLanzaReglaDominio() {
        guardarRegaloElegido();
        enviar.ejecutar(id);

        assertThrows(ReglaDominioException.class, () -> confirmar.ejecutar(id, UUID.randomUUID()));
        assertEquals(EstadoRegalo.ENVIADO, regaloRepository.obtenerPorId(id).get().getEstado());
    }

    @Test
    void regaloInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> enviar.ejecutar(UUID.randomUUID()));
        assertThrows(NoSuchElementException.class, () -> confirmar.ejecutar(UUID.randomUUID(), destinatarioId));
    }
}
