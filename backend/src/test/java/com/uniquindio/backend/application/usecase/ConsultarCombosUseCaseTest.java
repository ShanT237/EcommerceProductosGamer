package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.application.dto.response.ComboDetalleResponse;
import com.uniquindio.backend.domain.entity.Combo;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.infrastructure.persistence.ComboRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsultarCombosUseCaseTest {

    private ComboRepositoryEnMemoria comboRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private ConsultarCombosUseCase useCase;
    private UUID vendedorId;

    @BeforeEach
    void setUp() {
        comboRepository = new ComboRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new ConsultarCombosUseCase(comboRepository, productoRepository);
        vendedorId = UUID.randomUUID();
    }

    private Producto producto(UUID duenoId, String nombre, double precio) {
        Producto p = Producto.publicar(UUID.randomUUID(), duenoId, nombre, "Desc", precio, 10,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private Combo combo(UUID duenoId, double descuento, Producto... productos) {
        Combo c = Combo.crear(UUID.randomUUID(), duenoId, "Combo", "Desc", descuento,
                List.of(productos).stream().map(Producto::getId).toList());
        comboRepository.guardar(c);
        return c;
    }

    @Test
    void todosExcluyeCombosConUnProductoPausadoOEliminado() {
        Producto a = producto(vendedorId, "A", 100);
        Producto b = producto(vendedorId, "B", 50);
        Producto pausado = producto(vendedorId, "Pausado", 70);
        Producto eliminado = producto(vendedorId, "Eliminado", 70);
        Combo visible = combo(vendedorId, 10, a, b);
        combo(vendedorId, 10, a, pausado);
        combo(vendedorId, 10, b, eliminado);
        pausado.pausar();
        eliminado.eliminar(false);

        List<UUID> ids = useCase.todos().stream().map(c -> c.combo().getId()).toList();

        assertEquals(List.of(visible.getId()), ids);
    }

    @Test
    void todosExcluyeCombosConProductosQueYaNoExisten() {
        Producto a = producto(vendedorId, "A", 100);
        Combo huerfano = Combo.crear(UUID.randomUUID(), vendedorId, "Roto", "Desc", 10,
                List.of(a.getId(), UUID.randomUUID()));
        comboRepository.guardar(huerfano);

        assertTrue(useCase.todos().isEmpty());
    }

    @Test
    void todosIncluyeCombosDeTodosLosVendedores() {
        UUID otro = UUID.randomUUID();
        combo(vendedorId, 10, producto(vendedorId, "A", 100), producto(vendedorId, "B", 50));
        combo(otro, 5, producto(otro, "C", 100), producto(otro, "D", 50));

        assertEquals(2, useCase.todos().size());
    }

    @Test
    void deVendedorDevuelveSoloLosPropiosAunqueTenganProductosPausados() {
        UUID otro = UUID.randomUUID();
        Producto a = producto(vendedorId, "A", 100);
        Producto pausado = producto(vendedorId, "Pausado", 70);
        Combo propio = combo(vendedorId, 10, a, pausado);
        combo(otro, 5, producto(otro, "C", 100), producto(otro, "D", 50));
        pausado.pausar();

        List<ComboConProductos> resultado = useCase.deVendedor(vendedorId);

        assertEquals(1, resultado.size());
        assertEquals(propio.getId(), resultado.get(0).combo().getId());
        assertEquals(2, resultado.get(0).productos().size());
    }

    @Test
    void deVendedorSinCombosDevuelveListaVacia() {
        assertTrue(useCase.deVendedor(vendedorId).isEmpty());
    }

    @Test
    void calculaPrecioSinYConDescuento() {
        Producto a = producto(vendedorId, "A", 100);
        Producto b = producto(vendedorId, "B", 50);
        combo(vendedorId, 20, a, b);

        ComboDetalleResponse detalle = ComboDetalleResponse.desde(useCase.todos().get(0));

        assertEquals(150.0, detalle.precioSinDescuento(), 0.001);
        assertEquals(120.0, detalle.precioConDescuento(), 0.001);
        assertEquals(2, detalle.productos().size());
        assertEquals("A", detalle.productos().get(0).nombre());
    }
}
