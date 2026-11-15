package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ConflictoException;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.CuponRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CrearCuponUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private CuponRepositoryEnMemoria cuponRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private CrearCuponUseCase useCase;
    private Vendedor vendedor;

    @BeforeEach
    void setUp() {
        cuponRepository = new CuponRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new CrearCuponUseCase(cuponRepository, vendedorRepository, reloj);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
    }

    @Test
    void creaNormalizandoElCodigoAMayusculas() {
        UUID id = UUID.randomUUID();

        Cupon cupon = useCase.ejecutar(id, vendedor.getId(), " tienda10 ", 10, HOY.plusDays(30), 5);

        assertEquals(id, cupon.getId());
        assertEquals("TIENDA10", cupon.getCodigo());
        assertEquals(vendedor.getId(), cupon.getVendedorId());
        assertEquals(0, cupon.getUsos());
        assertEquals(5, cupon.getUsosMaximos());
        assertTrue(cuponRepository.obtenerPorCodigo("tienda10").isPresent());
    }

    @Test
    void elCuponPuedeVencerHoy() {
        assertDoesNotThrow(() -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "HOY10", 10, HOY, 1));
    }

    @Test
    void codigoDuplicadoLanzaConflictoSinImportarMayusculas() {
        useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "TIENDA10", 10, HOY.plusDays(30), 5);

        assertThrows(ConflictoException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "tienda10", 20, HOY.plusDays(60), 9));
        assertEquals(10, cuponRepository.obtenerPorCodigo("TIENDA10").get().getPorcentaje());
    }

    @Test
    void elCodigoDeOtroVendedorTambienCuentaComoDuplicado() {
        Vendedor otro = Vendedor.registrar(UUID.randomUUID(), "Otra", "o@t.com", TipoVendedor.EMPRESA, "901");
        vendedorRepository.guardar(otro);
        useCase.ejecutar(UUID.randomUUID(), otro.getId(), "TIENDA10", 10, HOY.plusDays(30), 5);

        assertThrows(ConflictoException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "TIENDA10", 10, HOY.plusDays(30), 5));
    }

    @Test
    void vencimientoPasadoSeRechazaYNoGuarda() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "VIEJO10", 10, HOY.minusDays(1), 5));

        assertTrue(cuponRepository.obtenerPorCodigo("VIEJO10").isEmpty());
    }

    @Test
    void datosInvalidosSeRechazan() {
        UUID id = UUID.randomUUID();
        UUID v = vendedor.getId();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, v, " ", 10, HOY.plusDays(5), 5));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, v, "X", 0, HOY.plusDays(5), 5));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, v, "X", 100, HOY.plusDays(5), 5));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(id, v, "X", 10, HOY.plusDays(5), 0));
    }

    @Test
    void vendedorDadoDeBajaRechaza() {
        vendedor.darDeBaja();

        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), vendedor.getId(), "TIENDA10", 10, HOY.plusDays(30), 5));
        assertTrue(cuponRepository.obtenerPorCodigo("TIENDA10").isEmpty());
    }

    @Test
    void vendedorInexistenteRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), UUID.randomUUID(), "TIENDA10", 10, HOY.plusDays(30), 5));
    }
}
