package com.uniquindio.backend.infrastructure.scheduling;

import com.uniquindio.backend.application.usecase.ExpirarComprasPendientesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ComprasSchedulerTest {

    private ExpirarComprasPendientesUseCase useCase;
    private ComprasScheduler scheduler;

    @BeforeEach
    void setUp() {
        useCase = Mockito.mock(ExpirarComprasPendientesUseCase.class);
        scheduler = new ComprasScheduler(useCase);
    }

    @Test
    void expirarPendientesInvocaElCasoDeUso() {
        scheduler.expirarPendientes();

        verify(useCase).ejecutar();
    }

    @Test
    void expirarPendientesNoPropagaLaExcepcionDelCasoDeUso() {
        when(useCase.ejecutar()).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(scheduler::expirarPendientes);
    }
}
