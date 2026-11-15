package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.infrastructure.persistence.CuponRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarCuponUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private CuponRepositoryEnMemoria cuponRepository;
    private ConsultarCuponUseCase useCase;
    private final UUID usuarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        cuponRepository = new CuponRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new ConsultarCuponUseCase(cuponRepository, reloj);
    }

    @Test
    void devuelveElCuponSinConsumirUsos() {
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), "GAMER10", 10, HOY.plusDays(30), 3));

        Cupon cupon = useCase.ejecutar("gamer10", usuarioId);

        assertEquals("GAMER10", cupon.getCodigo());
        assertEquals(0, cupon.getUsos());
        assertEquals(0, cuponRepository.obtenerPorCodigo("GAMER10").get().getUsos());
    }

    @Test
    void vencidoRechaza() {
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), "VIEJO10", 10, HOY.minusDays(1), 3));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar("VIEJO10", usuarioId));
    }

    @Test
    void agotadoRechaza() {
        Cupon agotado = Cupon.crear(UUID.randomUUID(), "UNICO10", 10, HOY.plusDays(30), 1);
        agotado.registrarUso();
        cuponRepository.guardar(agotado);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar("UNICO10", usuarioId));
    }

    @Test
    void yaUsadoPorElCompradorRechazaPeroOtroCompradorPuedeVerlo() {
        Cupon cupon = Cupon.crear(UUID.randomUUID(), "GAMER10", 10, HOY.plusDays(30), 3);
        cuponRepository.guardar(cupon);
        cuponRepository.registrarUsoDe(cupon.getId(), usuarioId);

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar("GAMER10", usuarioId));
        assertEquals("GAMER10", useCase.ejecutar("GAMER10", UUID.randomUUID()).getCodigo());
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar("NOEXISTE", usuarioId));
    }
}
