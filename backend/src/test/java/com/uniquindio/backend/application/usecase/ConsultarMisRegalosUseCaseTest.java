package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarMisRegalosUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private ConsultarMisRegalosUseCase useCase;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        useCase = new ConsultarMisRegalosUseCase(regaloRepository);
    }

    @Test
    void misRegalosSoloDelUsuario() {
        UUID usuario = UUID.randomUUID();
        Regalo suyo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        suyo.elegir(usuario, false);
        Regalo deOtro = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        deOtro.elegir(UUID.randomUUID(), false);
        Regalo sinDueno = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        List.of(suyo, deOtro, sinDueno).forEach(regaloRepository::guardar);

        List<Regalo> resultado = useCase.ejecutar(usuario);

        assertEquals(1, resultado.size());
        assertEquals(suyo.getId(), resultado.get(0).getId());
    }

    @Test
    void usuarioSinRegalosDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }
}
