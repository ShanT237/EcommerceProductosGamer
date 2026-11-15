package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Cupon;
import com.uniquindio.backend.infrastructure.persistence.CuponRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarCuponesVendedorUseCaseTest {

    private static final LocalDate VENCE = LocalDate.of(2026, 12, 31);

    private CuponRepositoryEnMemoria cuponRepository;
    private ConsultarCuponesVendedorUseCase useCase;

    @BeforeEach
    void setUp() {
        cuponRepository = new CuponRepositoryEnMemoria();
        useCase = new ConsultarCuponesVendedorUseCase(cuponRepository);
    }

    @Test
    void devuelveSoloLosCuponesPropiosOrdenadosPorCodigoSinLosGlobalesNiLosAjenos() {
        UUID vendedorId = UUID.randomUUID();
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), vendedorId, "ZETA10", 10, VENCE, 5));
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), vendedorId, "ALFA10", 10, VENCE, 5));
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), UUID.randomUUID(), "AJENO10", 10, VENCE, 5));
        cuponRepository.guardar(Cupon.crear(UUID.randomUUID(), "GLOBAL10", 10, VENCE, 5));

        List<Cupon> cupones = useCase.ejecutar(vendedorId);

        assertEquals(List.of("ALFA10", "ZETA10"), cupones.stream().map(Cupon::getCodigo).toList());
    }

    @Test
    void sinCuponesDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }
}
