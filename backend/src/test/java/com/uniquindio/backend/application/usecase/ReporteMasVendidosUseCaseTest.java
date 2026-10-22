package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReporteMasVendidosUseCaseTest {

    @Test
    void delegaVendedorYLimiteAlPuertoDeConsulta() {
        UUID vendedorId = UUID.randomUUID();
        ProductoVendidoResponse producto = new ProductoVendidoResponse(
                UUID.randomUUID(), "Mouse", 5, 4.5);
        AtomicReference<String> argumentos = new AtomicReference<>();
        ReporteMasVendidosUseCase useCase = new ReporteMasVendidosUseCase((vendedor, top) -> {
            argumentos.set(vendedor + ":" + top);
            return List.of(producto);
        });

        assertEquals(List.of(producto), useCase.ejecutar(vendedorId, 10));
        assertEquals(vendedorId + ":10", argumentos.get());
    }

    @Test
    void rechazaUnLimiteNoPositivoSinConsultarElRepositorio() {
        ReporteMasVendidosUseCase useCase = new ReporteMasVendidosUseCase(
                (vendedor, top) -> { throw new AssertionError("No debe consultar el reporte"); });

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(null, 0));
    }
}
