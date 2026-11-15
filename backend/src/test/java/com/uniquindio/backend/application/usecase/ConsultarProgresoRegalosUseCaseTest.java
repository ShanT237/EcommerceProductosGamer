package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProgresoRegalosResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.RegaloRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsultarProgresoRegalosUseCaseTest {

    private CompraRepositoryEnMemoria compraRepository;
    private RegaloRepositoryEnMemoria regaloRepository;
    private ConsultarProgresoRegalosUseCase useCase;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        regaloRepository = new RegaloRepositoryEnMemoria();
        useCase = new ConsultarProgresoRegalosUseCase(compraRepository, regaloRepository);
        usuarioId = UUID.randomUUID();
    }

    private void comprasCompletadas(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1,
                    LocalDateTime.of(2026, 11, 14, 10, 0), new Precio(100));
            compra.confirmar();
            compraRepository.guardar(compra);
        }
    }

    private void regaloElegido() {
        Regalo regalo = Regalo.crear(UUID.randomUUID(), UUID.randomUUID(), null);
        regalo.elegir(usuarioId, false);
        regaloRepository.guardar(regalo);
    }

    @Test
    void sinComprasDevuelveCeros() {
        ProgresoRegalosResponse progreso = useCase.ejecutar(usuarioId);

        assertEquals(0, progreso.comprasCompletadas());
        assertEquals(0, progreso.regalosAsignados());
        assertEquals(0, progreso.regalosDisponibles());
        assertEquals(5, progreso.comprasPorRegalo());
        assertEquals(5, progreso.comprasParaSiguiente());
    }

    @Test
    @DisplayName("siete compras → regalosDisponibles 1 y comprasParaSiguiente 3")
    void sieteComprasDanUnRegaloYFaltanTres() {
        comprasCompletadas(7);

        ProgresoRegalosResponse progreso = useCase.ejecutar(usuarioId);

        assertEquals(7, progreso.comprasCompletadas());
        assertEquals(1, progreso.regalosDisponibles());
        assertEquals(3, progreso.comprasParaSiguiente());
    }

    @Test
    void restaLosAsignados() {
        comprasCompletadas(10);
        regaloElegido();

        ProgresoRegalosResponse progreso = useCase.ejecutar(usuarioId);

        assertEquals(1, progreso.regalosAsignados());
        assertEquals(1, progreso.regalosDisponibles());   // 10 / 5 = 2, menos 1 ya elegido
        assertEquals(5, progreso.comprasParaSiguiente());
    }

    @Test
    void nuncaDevuelveRegalosDisponiblesNegativos() {
        comprasCompletadas(2);
        regaloElegido();

        assertEquals(0, useCase.ejecutar(usuarioId).regalosDisponibles());
    }

    @Test
    void lasComprasPendientesNoCuentan() {
        comprasCompletadas(4);
        compraRepository.guardar(Compra.realizar(UUID.randomUUID(), usuarioId, UUID.randomUUID(), 1,
                LocalDateTime.of(2026, 11, 14, 10, 0), new Precio(100)));

        ProgresoRegalosResponse progreso = useCase.ejecutar(usuarioId);

        assertEquals(4, progreso.comprasCompletadas());
        assertEquals(0, progreso.regalosDisponibles());
        assertEquals(1, progreso.comprasParaSiguiente());
    }
}
