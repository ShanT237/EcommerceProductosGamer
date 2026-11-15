package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.repository.CompraRepository;
import com.uniquindio.backend.domain.repository.ProductoRepository;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CancelarYReembolsarUseCaseTest {

    private CompraRepository compraRepository;
    private ProductoRepository productoRepository;
    private CancelarCompraUseCase cancelarUseCase;
    private ConfirmarCompraUseCase confirmarUseCase;
    private SolicitarReembolsoUseCase reembolsoUseCase;

    private static final ZoneId ZONA = ZoneId.systemDefault();
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 9, 18, 10, 0);
    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        cancelarUseCase = new CancelarCompraUseCase(compraRepository, productoRepository);
        confirmarUseCase = new ConfirmarCompraUseCase(compraRepository);
        reembolsoUseCase = new SolicitarReembolsoUseCase(compraRepository, productoRepository, RELOJ);
    }

    @Test
    @DisplayName("Cancelar una compra PENDIENTE cambia estado a CANCELADA y restituye stock al producto")
    void cancelarCompraRestituyeStock() {
        UUID usuarioId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Silla Gamer", "Ergonómica", 200.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(2); // Quedan 8
        productoRepository.guardar(producto);

        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 2, AHORA.minusHours(24), new Precio(200.0));
        compraRepository.guardar(compra);

        Compra compraCancelada = cancelarUseCase.ejecutar(compra.getId(), usuarioId);

        assertEquals(EstadoCompra.CANCELADA, compraCancelada.getEstado());
        assertEquals(10, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }

    @Test
    @DisplayName("Reembolsar una compra COMPLETADA cambia estado a REEMBOLSADA y restituye stock al producto")
    void reembolsarCompraRestituyeStock() {
        UUID usuarioId = UUID.randomUUID();
        UUID vendedorId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Headset Gamer", "7.1 Surround", 80.0, 5, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(1); // Quedan 4
        productoRepository.guardar(producto);

        Compra compra = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, AHORA.minusHours(24), new Precio(80.0));
        compra.confirmar(); // PENDIENTE -> COMPLETADA
        compraRepository.guardar(compra);

        Compra compraReembolsada = reembolsoUseCase.ejecutar(compra.getId(), usuarioId);

        assertEquals(EstadoCompra.REEMBOLSADA, compraReembolsada.getEstado());
        assertEquals(5, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }

    @Test
    @DisplayName("Una compra que pertenece a un pedido no se confirma ni se cancela por separado (422) y nada cambia")
    void compraDeUnPedidoSeRechazaConMensajeClaro() {
        UUID usuarioId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "RGB", 50.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(1); // Queda 9
        productoRepository.guardar(producto);
        Compra linea = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, AHORA.minusHours(1),
                new Precio(50.0), UUID.randomUUID());
        compraRepository.guardar(linea);

        ReglaDominioException alConfirmar = assertThrows(ReglaDominioException.class,
                () -> confirmarUseCase.ejecutar(linea.getId(), usuarioId));
        ReglaDominioException alCancelar = assertThrows(ReglaDominioException.class,
                () -> cancelarUseCase.ejecutar(linea.getId(), usuarioId));

        assertTrue(alConfirmar.getMessage().contains("pedido"));
        assertTrue(alCancelar.getMessage().contains("pedido"));
        assertEquals(EstadoCompra.PENDIENTE, compraRepository.obtenerPorId(linea.getId()).get().getEstado());
        assertEquals(9, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }

    @Test
    @DisplayName("Una línea de un pedido completado no se puede reembolsar por separado")
    void unaLineaDeUnPedidoCompletadaSeRechazaAlReembolsar() {
        UUID usuarioId = UUID.randomUUID();
        Producto producto = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Mouse", "RGB", 50.0, 10, Gama.MEDIA, Exclusividad.NORMAL);
        producto.reducirStock(1); // Queda 9
        productoRepository.guardar(producto);
        Compra linea = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, AHORA.minusHours(1),
                new Precio(50.0), UUID.randomUUID());
        linea.confirmar();
        compraRepository.guardar(linea);

        assertThrows(ReglaDominioException.class, () -> reembolsoUseCase.ejecutar(linea.getId(), usuarioId));

        assertEquals(EstadoCompra.COMPLETADA, compraRepository.obtenerPorId(linea.getId()).get().getEstado());
        assertEquals(9, productoRepository.obtenerPorId(producto.getId()).get().getStock());
    }
}
