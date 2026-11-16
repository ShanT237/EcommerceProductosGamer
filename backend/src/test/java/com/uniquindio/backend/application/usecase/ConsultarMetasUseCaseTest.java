package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.MetaRecompensa;
import com.uniquindio.backend.infrastructure.persistence.MetaRecompensaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarMetasUseCaseTest {

    private MetaRecompensaRepositoryEnMemoria metaRepository;
    private ConsultarMetasUseCase useCase;

    @BeforeEach
    void setUp() {
        metaRepository = new MetaRecompensaRepositoryEnMemoria();
        useCase = new ConsultarMetasUseCase(metaRepository);
    }

    @Test
    void devuelveSoloLasActivasOrdenadasPorUmbral() {
        MetaRecompensa diez = MetaRecompensa.crear(UUID.randomUUID(), 10, UUID.randomUUID());
        MetaRecompensa tres = MetaRecompensa.crear(UUID.randomUUID(), 3, UUID.randomUUID());
        MetaRecompensa cinco = MetaRecompensa.crear(UUID.randomUUID(), 5, UUID.randomUUID());
        MetaRecompensa inactiva = MetaRecompensa.crear(UUID.randomUUID(), 1, UUID.randomUUID());
        inactiva.desactivar();
        metaRepository.guardar(diez);
        metaRepository.guardar(tres);
        metaRepository.guardar(cinco);
        metaRepository.guardar(inactiva);

        List<MetaRecompensa> resultado = useCase.ejecutar();

        assertEquals(List.of(tres.getId(), cinco.getId(), diez.getId()),
                resultado.stream().map(MetaRecompensa::getId).toList());
    }

    @Test
    void sinMetasDevuelveListaVacia() {
        assertTrue(useCase.ejecutar().isEmpty());
    }
}
