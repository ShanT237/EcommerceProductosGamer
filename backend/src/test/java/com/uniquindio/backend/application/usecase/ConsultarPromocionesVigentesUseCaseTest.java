package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.infrastructure.persistence.PromocionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarPromocionesVigentesUseCaseTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    private PromocionRepositoryEnMemoria promocionRepository;
    private ConsultarPromocionesVigentesUseCase useCase;

    @BeforeEach
    void setUp() {
        promocionRepository = new PromocionRepositoryEnMemoria();
        Clock reloj = Clock.fixed(HOY.atTime(12, 0).atZone(ZONA).toInstant(), ZONA);
        useCase = new ConsultarPromocionesVigentesUseCase(promocionRepository, reloj);
    }

    private Promocion promocion(UUID productoId, double porcentaje, LocalDate desde, LocalDate hasta) {
        Promocion p = Promocion.programar(UUID.randomUUID(), productoId, porcentaje, desde, hasta);
        promocionRepository.guardar(p);
        return p;
    }

    @Test
    void devuelveSoloLaPromocionVigenteDeLosProductosPedidos() {
        UUID conPromo = UUID.randomUUID();
        UUID vencida = UUID.randomUUID();
        UUID futura = UUID.randomUUID();
        UUID noPedido = UUID.randomUUID();
        Promocion vigente = promocion(conPromo, 15, HOY.minusDays(2), HOY.plusDays(10));
        promocion(vencida, 10, HOY.minusDays(20), HOY.minusDays(1));
        promocion(futura, 10, HOY.plusDays(1), HOY.plusDays(5));
        promocion(noPedido, 30, HOY.minusDays(1), HOY.plusDays(1));

        Map<UUID, Promocion> resultado = useCase.ejecutar(List.of(conPromo, vencida, futura));

        assertEquals(1, resultado.size());
        assertEquals(vigente, resultado.get(conPromo));
    }

    @Test
    void losLimitesDeLaVigenciaSonInclusivos() {
        UUID empiezaHoy = UUID.randomUUID();
        UUID terminaHoy = UUID.randomUUID();
        promocion(empiezaHoy, 10, HOY, HOY.plusDays(3));
        promocion(terminaHoy, 10, HOY.minusDays(3), HOY);

        Map<UUID, Promocion> resultado = useCase.ejecutar(List.of(empiezaHoy, terminaHoy));

        assertTrue(resultado.containsKey(empiezaHoy));
        assertTrue(resultado.containsKey(terminaHoy));
    }

    @Test
    void unProductoSinPromocionNoApareceEnElMapa() {
        UUID sinPromo = UUID.randomUUID();

        Map<UUID, Promocion> resultado = useCase.ejecutar(List.of(sinPromo));

        assertFalse(resultado.containsKey(sinPromo));
        assertTrue(resultado.isEmpty());
    }

    @Test
    void sinProductosDevuelveUnMapaVacio() {
        assertTrue(useCase.ejecutar(List.of()).isEmpty());
    }
}
