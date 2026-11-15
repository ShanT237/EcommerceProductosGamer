package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.valueobject.EstadoRegalo;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarRegalosDisponiblesUseCaseTest {

    private RegaloRepositoryEnMemoria regaloRepository;
    private ConsultarRegalosDisponiblesUseCase useCase;

    @BeforeEach
    void setUp() {
        regaloRepository = new RegaloRepositoryEnMemoria();
        useCase = new ConsultarRegalosDisponiblesUseCase(regaloRepository);
    }

    @Test
    void disponiblesExcluyeLosElegidos() {
        Regalo disponible = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        Regalo elegido = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        elegido.elegir(UUID.randomUUID(), false);
        Regalo cancelado = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        cancelado.cancelarRegalo();
        List.of(disponible, elegido, cancelado).forEach(regaloRepository::guardar);

        List<Regalo> resultado = useCase.ejecutar();

        assertEquals(1, resultado.size());
        assertEquals(disponible.getId(), resultado.get(0).getId());
        assertEquals(EstadoRegalo.DISPONIBLE, resultado.get(0).getEstado());
    }

    @Test
    void sinRegalosDevuelveListaVacia() {
        assertTrue(useCase.ejecutar().isEmpty());
    }
}
