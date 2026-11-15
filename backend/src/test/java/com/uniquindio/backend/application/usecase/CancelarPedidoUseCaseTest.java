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

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelarPedidoUseCaseTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private CompraRepositoryEnMemoria compraRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private CancelarPedidoUseCase useCase;
    private UUID usuarioId;
    private UUID pedidoId;
    private Producto teclado;
    private Producto mouse;
    private Compra linea1;
    private Compra linea2;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new CancelarPedidoUseCase(compraRepository, productoRepository);
        usuarioId = UUID.randomUUID();
        pedidoId = UUID.randomUUID();

        // Stock ya descontado por la creación del pedido: 5 - 2 = 3 y 5 - 1 = 4
        teclado = producto("Teclado", 100, 5);
        mouse = producto("Mouse", 50, 5);
        teclado.reducirStock(2);
        mouse.reducirStock(1);
        linea1 = linea(teclado, 2);
        linea2 = linea(mouse, 1);
    }

    private Producto producto(String nombre, double precio, int stock) {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", precio, stock,
                Gama.MEDIA, Exclusividad.NORMAL);
        productoRepository.guardar(p);
        return p;
    }

    private Compra linea(Producto producto, int cantidad) {
        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), cantidad, AHORA,
                new Precio(producto.getPrecio()), pedidoId);
        compraRepository.guardar(compra);
        return compra;
    }

    @Test
    void cancelaTodasYRestituyeElStock() {
        List<Compra> canceladas = useCase.ejecutar(pedidoId, usuarioId);

        assertEquals(2, canceladas.size());
        assertTrue(canceladas.stream().allMatch(c -> c.getEstado() == EstadoCompra.CANCELADA));
        assertEquals(EstadoCompra.CANCELADA, compraRepository.obtenerPorId(linea1.getId()).get().getEstado());
        assertEquals(EstadoCompra.CANCELADA, compraRepository.obtenerPorId(linea2.getId()).get().getEstado());
        assertEquals(5, productoRepository.obtenerPorId(teclado.getId()).get().getStock());
        assertEquals(5, productoRepository.obtenerPorId(mouse.getId()).get().getStock());
    }

    @Test
    void pedidoYaConfirmadoRechazaYNoCambiaNada() {
        linea1.confirmar();
        linea2.confirmar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, usuarioId));

        assertEquals(EstadoCompra.COMPLETADA, linea1.getEstado());
        assertEquals(EstadoCompra.COMPLETADA, linea2.getEstado());
        assertEquals(3, teclado.getStock());
        assertEquals(4, mouse.getStock());
    }

    @Test
    void siUnaLineaYaNoEstaPendienteNoCambiaNingunaNiElStock() {
        linea2.confirmar();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, usuarioId));

        assertEquals(EstadoCompra.PENDIENTE, linea1.getEstado());
        assertEquals(3, teclado.getStock());
        assertEquals(4, mouse.getStock());
    }

    @Test
    void pedidoAjenoRechazaYNoCambiaNada() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pedidoId, UUID.randomUUID()));

        assertEquals(EstadoCompra.PENDIENTE, linea1.getEstado());
        assertEquals(EstadoCompra.PENDIENTE, linea2.getEstado());
        assertEquals(3, teclado.getStock());
        assertEquals(4, mouse.getStock());
    }

    @Test
    void inexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(UUID.randomUUID(), usuarioId));
    }
}
