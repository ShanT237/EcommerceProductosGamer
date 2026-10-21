package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ReporteGlobalResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReporteGlobalUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private ReporteGlobalUseCase useCase;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new ReporteGlobalUseCase(compraRepository, productoRepository, vendedorRepository);
    }

    @Test
    void marketplaceVacioDevuelveCeros() {
        ReporteGlobalResponse r = useCase.ejecutar();

        assertEquals(0, r.comprasCompletadas());
        assertEquals(0, r.ingresos());
        assertEquals(0, r.productosActivos());
        assertEquals(0, r.vendedoresActivos());
    }

    @Test
    void cuentaSoloCompletadasEnIngresosYSoloActivosEnProductosYVendedores() {
        Vendedor activo = Vendedor.registrar(UUID.randomUUID(), "A", "a@g.com", TipoVendedor.EMPRESA, "1");
        Vendedor baja = Vendedor.registrar(UUID.randomUUID(), "B", "b@g.com", TipoVendedor.PERSONA_NATURAL, "2");
        baja.darDeBaja();
        vendedorRepository.guardar(activo);
        vendedorRepository.guardar(baja);

        Producto vivo = Producto.publicar(UUID.randomUUID(), activo.getId(), "Vivo", "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
        Producto borrado = Producto.publicar(UUID.randomUUID(), activo.getId(), "Borrado", "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
        borrado.eliminar(false);
        productoRepository.guardar(vivo);
        productoRepository.guardar(borrado);

        Compra completada = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), vivo.getId(), 2, FECHA, new Precio(100));
        completada.confirmar();
        Compra pendiente = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), vivo.getId(), 1, FECHA, new Precio(100));
        Compra reembolsada = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), vivo.getId(), 1, FECHA, new Precio(100));
        reembolsada.confirmar();
        reembolsada.solicitarReembolso(FECHA.plusHours(1));
        compraRepository.guardar(completada);
        compraRepository.guardar(pendiente);
        compraRepository.guardar(reembolsada);

        ReporteGlobalResponse r = useCase.ejecutar();

        assertEquals(1, r.comprasCompletadas());
        assertEquals(200, r.ingresos(), 0.001);
        assertEquals(1, r.productosActivos());
        assertEquals(1, r.vendedoresActivos());
    }
}
