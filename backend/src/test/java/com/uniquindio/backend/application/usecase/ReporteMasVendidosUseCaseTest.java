package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ProductoVendidoResponse;
import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ResenaRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReporteMasVendidosUseCaseTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private ResenaRepositoryEnMemoria resenaRepository;
    private ReporteMasVendidosUseCase useCase;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        resenaRepository = new ResenaRepositoryEnMemoria();
        useCase = new ReporteMasVendidosUseCase(productoRepository, compraRepository, resenaRepository);
        vendedorId = UUID.randomUUID();
    }

    private Producto producto(UUID dueno, String nombre) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, nombre, "Desc", 100, 50,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private void vender(Producto p, int cantidad) {
        Compra c = Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), p.getId(), cantidad, FECHA, new Precio(100));
        c.confirmar();
        compraRepository.guardar(c);
    }

    private void resenar(Producto p, int estrellas) {
        resenaRepository.guardar(Resena.crear(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                p.getId().toString(), UUID.randomUUID().toString(), true, new Calificacion(estrellas), "ok"));
    }

    @Test
    void ordenaPorUnidadesVendidasDescendente() {
        Producto poco = producto(vendedorId, "Poco");
        Producto mucho = producto(vendedorId, "Mucho");
        vender(poco, 1);
        vender(mucho, 3);
        vender(mucho, 2);

        List<ProductoVendidoResponse> r = useCase.ejecutar(null, 10);

        assertEquals(List.of("Mucho", "Poco"), r.stream().map(ProductoVendidoResponse::nombre).toList());
        assertEquals(5, r.get(0).unidades());
    }

    @Test
    void desempataPorCalificacionPromedio() {
        Producto a = producto(vendedorId, "A");
        Producto b = producto(vendedorId, "B");
        vender(a, 2);
        vender(b, 2);
        resenar(a, 3);
        resenar(b, 5);

        List<ProductoVendidoResponse> r = useCase.ejecutar(null, 10);

        assertEquals("B", r.get(0).nombre());
        assertEquals(5.0, r.get(0).calificacionPromedio(), 0.001);
    }

    @Test
    void topLimitaLosResultadosYFiltraPorVendedor() {
        Producto propio1 = producto(vendedorId, "P1");
        Producto propio2 = producto(vendedorId, "P2");
        Producto ajeno = producto(UUID.randomUUID(), "Ajeno");
        vender(propio1, 1);
        vender(propio2, 2);
        vender(ajeno, 9);

        assertEquals(1, useCase.ejecutar(vendedorId, 1).size());
        assertEquals("P2", useCase.ejecutar(vendedorId, 1).get(0).nombre());
        assertEquals("Ajeno", useCase.ejecutar(null, 1).get(0).nombre());
    }

    @Test
    void ignoraComprasNoCompletadasYProductosSinVentas() {
        Producto p = producto(vendedorId, "P");
        producto(vendedorId, "SinVentas");
        compraRepository.guardar(Compra.realizar(UUID.randomUUID(), UUID.randomUUID(), p.getId(), 4, FECHA,
                new Precio(100)));

        assertTrue(useCase.ejecutar(null, 10).isEmpty());
    }

    @Test
    void topNoPositivoSeRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(null, 0));
    }
}
