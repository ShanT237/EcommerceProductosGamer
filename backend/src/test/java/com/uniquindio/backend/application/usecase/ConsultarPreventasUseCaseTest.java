package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarPreventasUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 15);

    private PreventaRepositoryEnMemoria repositorio;
    private ConsultarPreventasUseCase casoDeUso;

    @BeforeEach
    void setUp() {
        repositorio = new PreventaRepositoryEnMemoria();
        ZoneId zona = ZoneId.of("America/Bogota");
        Clock clock = Clock.fixed(HOY.atStartOfDay(zona).toInstant(), zona);
        casoDeUso = new ConsultarPreventasUseCase(repositorio, clock);
    }

    private Preventa preventa(LocalDate fecha) {
        Preventa preventa = Preventa.reconstruir(UUID.randomUUID(), UUID.randomUUID(), fecha, "Lanzamiento");
        repositorio.guardar(preventa);
        return preventa;
    }

    @Test
    void devuelveSoloPreventasConLanzamientoFuturoOrdenadas() {
        Preventa lejana = preventa(HOY.plusDays(10));
        Preventa proxima = preventa(HOY.plusDays(1));
        preventa(HOY);
        preventa(HOY.minusDays(1));

        List<Preventa> resultado = casoDeUso.ejecutar();

        assertEquals(List.of(proxima.getId(), lejana.getId()), resultado.stream().map(Preventa::getId).toList());
    }

    @Test
    void unaPreventaConFechaDeHoyNoEsFutura() {
        preventa(HOY);

        assertEquals(List.of(), casoDeUso.ejecutar());
    }
}
