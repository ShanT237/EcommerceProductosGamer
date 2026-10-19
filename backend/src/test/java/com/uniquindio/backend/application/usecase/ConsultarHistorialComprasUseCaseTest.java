package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarHistorialComprasUseCaseTest {

    private CompraRepositoryEnMemoria compraRepository;
    private ConsultarHistorialComprasUseCase useCase;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        useCase = new ConsultarHistorialComprasUseCase(compraRepository);
    }

    private Compra compra(UUID usuarioId, LocalDate fecha) {
        Compra c = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1, fecha.atStartOfDay(), new Precio(100));
        compraRepository.guardar(c);
        return c;
    }

    @Test
    void devuelveSoloComprasDelUsuarioOrdenadasPorFechaDesc() {
        UUID usuarioId = UUID.randomUUID();
        Compra vieja = compra(usuarioId, LocalDate.of(2026, 9, 1));
        Compra reciente = compra(usuarioId, LocalDate.of(2026, 10, 1));
        Compra media = compra(usuarioId, LocalDate.of(2026, 9, 15));
        compra(UUID.randomUUID(), LocalDate.of(2026, 10, 10));

        List<Compra> historial = useCase.ejecutar(usuarioId);

        assertEquals(List.of(reciente, media, vieja), historial);
    }

    @Test
    void usuarioSinComprasDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(UUID.randomUUID()).isEmpty());
    }
}
