package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SolicitarReembolsoPedidoUseCaseTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private final UUID usuarioId = UUID.randomUUID();
    private final UUID pedidoId = UUID.randomUUID();
    private CompraRepositoryEnMemoria compraRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private SolicitarReembolsoPedidoUseCase useCase;
    private Compra linea1;
    private Compra linea2;
    private Producto teclado;
    private Producto mouse;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock clock = Clock.fixed(AHORA.plusDays(1).atZone(zona).toInstant(), zona);
        useCase = new SolicitarReembolsoPedidoUseCase(compraRepository, productoRepository, clock);

        teclado = producto("Teclado");
        mouse = producto("Mouse");
        teclado.reducirStock(1);
        mouse.reducirStock(1);
        linea1 = compra(teclado);
        linea2 = compra(mouse);
        linea1.confirmar();
        linea2.confirmar();
    }

    private Producto producto(String nombre) {
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(producto);
        return producto;
    }

    private Compra compra(Producto producto) {
        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, AHORA,
                new Precio(100), pedidoId);
        compraRepository.guardar(compra);
        return compra;
    }

    @Test
    void reembolsaTodoElPedidoYRestituyeElStock() {
        var resultado = useCase.ejecutar(pedidoId, usuarioId);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(c -> c.getEstado() == EstadoCompra.REEMBOLSADA));
        assertEquals(5, teclado.getStock());
        assertEquals(5, mouse.getStock());
    }

    @Test
    void siUnaLineaNoPuedeReembolsarseNoCambiaNingunaNiElStock() {
        linea2.registrarResena();
        linea2.asignarPuntosPorResena();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, usuarioId));

        assertEquals(EstadoCompra.COMPLETADA, linea1.getEstado());
        assertEquals(EstadoCompra.COMPLETADA, linea2.getEstado());
        assertEquals(4, teclado.getStock());
        assertEquals(4, mouse.getStock());
    }

    @Test
    void reembolsoIndividualDeUnaLineaDelPedidoEsRechazado() {
        SolicitarReembolsoUseCase individual = new SolicitarReembolsoUseCase(compraRepository,
                productoRepository, Clock.fixed(AHORA.plusDays(1).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault()));

        assertThrows(ReglaDominioException.class, () -> individual.ejecutar(linea1.getId(), usuarioId));
        assertEquals(EstadoCompra.COMPLETADA, linea1.getEstado());
        assertEquals(4, teclado.getStock());
    }
}
