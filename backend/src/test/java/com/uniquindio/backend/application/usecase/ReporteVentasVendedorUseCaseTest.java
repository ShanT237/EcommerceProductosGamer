package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.application.dto.response.ReporteVentasResponse;
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
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReporteVentasVendedorUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private ReporteVentasVendedorUseCase useCase;
    private Vendedor vendedor;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new ReporteVentasVendedorUseCase(vendedorRepository, productoRepository, compraRepository);
        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
    }

    private Producto producto(UUID vendedorId, double precio) {
        Producto p = Producto.publicar(UUID.randomUUID(), vendedorId, "P", "Desc", precio, 10,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private Compra compra(Producto p, int cantidad, double precioUnitario, boolean completada) {
        Compra c = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), p.getId(), cantidad, FECHA,
                new Precio(precioUnitario));
        if (completada) {
            c.confirmar();
        }
        compraRepository.guardar(c);
        return c;
    }

    @Test
    void sumaIngresosUnidadesYComisionDelCincoPorCiento() {
        Producto a = producto(vendedor.getId(), 100);
        Producto b = producto(vendedor.getId(), 50);
        compra(a, 2, 100, true);
        compra(b, 1, 50, true);

        ReporteVentasResponse r = useCase.ejecutar(vendedor.getId());

        assertEquals(250, r.ingresos(), 0.001);
        assertEquals(3, r.unidades());
        assertEquals(12.5, r.comision(), 0.001);
    }

    @Test
    void ignoraPendientesReembolsadasYVentasDeOtrosVendedores() {
        Producto propio = producto(vendedor.getId(), 100);
        Producto ajeno = producto(UUID.randomUUID(), 100);
        compra(propio, 1, 100, false);
        compra(propio, 1, 100, true).solicitarReembolso(FECHA.plusHours(1));
        compra(ajeno, 5, 100, true);

        ReporteVentasResponse r = useCase.ejecutar(vendedor.getId());

        assertEquals(0, r.ingresos(), 0.001);
        assertEquals(0, r.unidades());
    }

    @Test
    void vendedorSinVentasDevuelveCerosYNoLanza() {
        ReporteVentasResponse r = useCase.ejecutar(vendedor.getId());

        assertEquals(0, r.ingresos());
        assertEquals(0, r.unidades());
        assertEquals(0, r.comision());
    }

    @Test
    void vendedorInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
