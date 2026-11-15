package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.usecase.ConsultarPromocionesDelVendedorUseCase.PromocionDeProducto;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PromocionRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConsultarPromocionesDelVendedorUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 14);

    private ProductoRepositoryEnMemoria productoRepository;
    private PromocionRepositoryEnMemoria promocionRepository;
    private ConsultarPromocionesDelVendedorUseCase useCase;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        promocionRepository = new PromocionRepositoryEnMemoria();
        useCase = new ConsultarPromocionesDelVendedorUseCase(productoRepository, promocionRepository);
        vendedorId = UUID.randomUUID();
    }

    private Producto publicar(UUID dueno, String nombre) {
        Producto p = Producto.publicar(UUID.randomUUID(), dueno, nombre, "Descripcion de " + nombre,
                100, 5, Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private Promocion programar(Producto producto, double porcentaje, LocalDate desde, LocalDate hasta) {
        Promocion pr = Promocion.programar(UUID.randomUUID(), producto.getId(), porcentaje, desde, hasta);
        promocionRepository.guardar(pr);
        return pr;
    }

    @Test
    void incluyeLasVencidasLasVigentesYLasFuturas() {
        Producto mouse = publicar(vendedorId, "Mouse");
        Promocion vencida = programar(mouse, 10, HOY.minusDays(30), HOY.minusDays(20));
        Promocion vigente = programar(mouse, 20, HOY.minusDays(1), HOY.plusDays(5));
        Promocion futura = programar(mouse, 30, HOY.plusDays(10), HOY.plusDays(20));

        List<PromocionDeProducto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(3, resultado.size());
        List<UUID> ids = resultado.stream().map(r -> r.promocion().getId()).toList();
        assertTrue(ids.containsAll(List.of(vencida.getId(), vigente.getId(), futura.getId())));
    }

    @Test
    void ordenaDeLaMasRecienteALaMasAntigua() {
        Producto mouse = publicar(vendedorId, "Mouse");
        Promocion antigua = programar(mouse, 10, HOY.minusDays(30), HOY.minusDays(20));
        Promocion futura = programar(mouse, 30, HOY.plusDays(10), HOY.plusDays(20));
        Promocion actual = programar(mouse, 20, HOY.minusDays(1), HOY.plusDays(5));

        List<PromocionDeProducto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(List.of(futura.getId(), actual.getId(), antigua.getId()),
                resultado.stream().map(r -> r.promocion().getId()).toList());
    }

    @Test
    void devuelveElNombreDelProductoDeCadaPromocion() {
        Producto mouse = publicar(vendedorId, "Mouse");
        Producto teclado = publicar(vendedorId, "Teclado");
        programar(mouse, 10, HOY, HOY.plusDays(5));
        programar(teclado, 20, HOY, HOY.plusDays(5));

        List<PromocionDeProducto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(2, resultado.size());
        resultado.forEach(r -> {
            String esperado = r.promocion().getProductoId().equals(mouse.getId()) ? "Mouse" : "Teclado";
            assertEquals(esperado, r.productoNombre());
        });
    }

    @Test
    void noIncluyeLasPromocionesDeOtrosVendedores() {
        Producto propio = publicar(vendedorId, "Mouse");
        Producto ajeno = publicar(UUID.randomUUID(), "Teclado");
        Promocion mia = programar(propio, 10, HOY, HOY.plusDays(5));
        programar(ajeno, 20, HOY, HOY.plusDays(5));

        List<PromocionDeProducto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(1, resultado.size());
        assertEquals(mia.getId(), resultado.get(0).promocion().getId());
    }

    @Test
    void noIncluyeLosProductosEliminados() {
        Producto vivo = publicar(vendedorId, "Mouse");
        Producto eliminado = publicar(vendedorId, "Teclado");
        Promocion deVivo = programar(vivo, 10, HOY, HOY.plusDays(5));
        programar(eliminado, 20, HOY, HOY.plusDays(5));
        eliminado.eliminar(false);

        List<PromocionDeProducto> resultado = useCase.ejecutar(vendedorId);

        assertEquals(1, resultado.size());
        assertEquals(deVivo.getId(), resultado.get(0).promocion().getId());
    }

    @Test
    void incluyeLasPromocionesDeUnProductoPausado() {
        Producto pausado = publicar(vendedorId, "Mouse");
        programar(pausado, 10, HOY, HOY.plusDays(5));
        pausado.pausar();

        assertEquals(1, useCase.ejecutar(vendedorId).size());
    }

    @Test
    void vendedorSinProductosDevuelveListaVacia() {
        assertTrue(useCase.ejecutar(vendedorId).isEmpty());
    }

    @Test
    void vendedorConProductosPeroSinPromocionesDevuelveListaVacia() {
        publicar(vendedorId, "Mouse");

        assertTrue(useCase.ejecutar(vendedorId).isEmpty());
    }
}
