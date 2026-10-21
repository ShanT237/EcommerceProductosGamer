package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.CuponRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AplicarCuponUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private CompraRepositoryEnMemoria compraRepository;
    private CuponRepositoryEnMemoria cuponRepository;
    private AplicarCuponUseCase useCase;
    private UUID usuarioId;
    private Compra compra;
    private Cupon cupon;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        cuponRepository = new CuponRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new AplicarCuponUseCase(compraRepository, cuponRepository, reloj);

        usuarioId = UUID.randomUUID();
        compra = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 2,
                LocalDateTime.of(2026, 10, 1, 9, 0), new Precio(100));
        compraRepository.guardar(compra);
        cupon = Cupon.crear(UUID.randomUUID(), "GAMER10", 10, HOY.plusDays(30), 2);
        cuponRepository.guardar(cupon);
    }

    @Test
    void aplicaElDescuentoAlPrecioUnitarioYConsumeUnUso() {
        Compra resultado = useCase.ejecutar(compra.getId(), usuarioId, "gamer10");

        assertEquals(90, resultado.getPrecioUnitario().valor(), 0.001);
        assertEquals(180, resultado.getSubtotal().valor(), 0.001);
        assertTrue(resultado.isCuponAplicado());
        assertEquals(1, cuponRepository.obtenerPorCodigo("GAMER10").get().getUsos());
    }

    @Test
    void rechazaUnSegundoCuponEnLaMismaCompraYNoConsumeUso() {
        useCase.ejecutar(compra.getId(), usuarioId, "GAMER10");

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, "GAMER10"));
        assertEquals(1, cuponRepository.obtenerPorCodigo("GAMER10").get().getUsos());
        assertEquals(90, compra.getPrecioUnitario().valor(), 0.001);
    }

    @Test
    void rechazaCompraQueNoEstaPendienteYNoConsumeUso() {
        compra.confirmar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, "GAMER10"));
        assertEquals(0, cuponRepository.obtenerPorCodigo("GAMER10").get().getUsos());
        assertEquals(EstadoCompra.COMPLETADA, compra.getEstado());
    }

    @Test
    void rechazaCuponVencido() {
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), "VIEJO", 10, HOY.minusDays(1), 5));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, "VIEJO"));
        assertFalse(compra.isCuponAplicado());
    }

    @Test
    void rechazaCuponAgotado() {
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), "UNICO", 10, HOY.plusDays(5), 1));
        Compra otra = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 1,
                LocalDateTime.of(2026, 10, 1, 9, 0), new Precio(50));
        compraRepository.guardar(otra);
        useCase.ejecutar(otra.getId(), otra.getUsuarioId(), "UNICO");

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, "UNICO"));
    }

    @Test
    void rechazaCompraAjena() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(compra.getId(), UUID.randomUUID(), "GAMER10"));
    }

    @Test
    void codigoInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(compra.getId(), usuarioId, "NOEXISTE"));
    }

    @Test
    void compraInexistenteLanzaReglaDominio() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), usuarioId, "GAMER10"));
    }
}
