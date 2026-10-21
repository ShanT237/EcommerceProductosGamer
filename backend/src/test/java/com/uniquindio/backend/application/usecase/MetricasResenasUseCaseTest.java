package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.MetricasResenasResponse;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Resena;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.Calificacion;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ResenaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MetricasResenasUseCaseTest {

    private ProductoRepositoryEnMemoria productoRepository;
    private ResenaRepositoryEnMemoria resenaRepository;
    private MetricasResenasUseCase useCase;
    private Vendedor vendedor;
    private Producto producto;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        resenaRepository = new ResenaRepositoryEnMemoria();
        VendedorRepositoryEnMemoria vendedorRepository = new VendedorRepositoryEnMemoria();
        useCase = new MetricasResenasUseCase(vendedorRepository, productoRepository, resenaRepository);
        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
        producto = producto(vendedor.getId());
    }

    private Producto producto(UUID dueno) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, "P", "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private void resenar(Producto p, int estrellas) {
        resenaRepository.guardar(Resena.crear(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                p.getId().toString(), UUID.randomUUID().toString(), true, new Calificacion(estrellas), "ok"));
    }

    @Test
    void sinResenasDevuelvePromedioCeroYDistribucionEnCeros() {
        MetricasResenasResponse m = useCase.ejecutar(vendedor.getId());

        assertEquals(0, m.promedio());
        assertEquals(0, m.total());
        assertEquals(5, m.distribucion().size());
        assertTrue(m.distribucion().values().stream().allMatch(v -> v == 0));
    }

    @Test
    void calculaPromedioDistribucionPositivasYNegativas() {
        Producto otro = producto(vendedor.getId());
        resenar(producto, 5);
        resenar(producto, 4);
        resenar(otro, 3);
        resenar(otro, 1);
        resenar(otro, 2);

        MetricasResenasResponse m = useCase.ejecutar(vendedor.getId());

        assertEquals(3.0, m.promedio(), 0.001);
        assertEquals(5, m.total());
        assertEquals(1L, m.distribucion().get(5));
        assertEquals(1L, m.distribucion().get(3));
        assertEquals(2, m.positivas());
        assertEquals(2, m.negativas());
    }

    @Test
    void ignoraResenasDeProductosDeOtrosVendedores() {
        resenar(producto(UUID.randomUUID()), 1);
        resenar(producto, 5);

        MetricasResenasResponse m = useCase.ejecutar(vendedor.getId());

        assertEquals(1, m.total());
        assertEquals(5.0, m.promedio(), 0.001);
    }

    @Test
    void vendedorInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID()));
    }
}
