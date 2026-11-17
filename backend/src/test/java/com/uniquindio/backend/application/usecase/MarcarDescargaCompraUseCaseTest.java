package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.domain.valueobject.TipoProducto;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MarcarDescargaCompraUseCaseTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private CompraRepositoryEnMemoria compraRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private MarcarDescargaCompraUseCase useCase;
    private UUID usuarioId;

    private Producto digital;
    private Producto fisico;
    private Compra compra;          // compra suelta COMPLETADA de un producto digital
    private Compra compraDeFisico;  // compra suelta COMPLETADA de un producto físico
    private Compra pendiente;       // compra suelta PENDIENTE de un producto digital

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        useCase = new MarcarDescargaCompraUseCase(compraRepository, productoRepository);
        usuarioId = UUID.randomUUID();

        digital = producto("Juego", TipoProducto.DIGITAL);
        fisico = producto("Teclado", TipoProducto.FISICO);
        compra = compraCompletada(digital, null);
        compraDeFisico = compraCompletada(fisico, null);
        pendiente = compra(digital, null);
    }

    private Producto producto(String nombre, TipoProducto tipo) {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL, "General", tipo);
        productoRepository.guardar(p);
        return p;
    }

    private Compra compra(Producto producto, UUID pedidoId) {
        Compra c = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), 1, FECHA,
                new Precio(producto.getPrecio()), pedidoId);
        compraRepository.guardar(c);
        return c;
    }

    private Compra compraCompletada(Producto producto, UUID pedidoId) {
        Compra c = compra(producto, pedidoId);
        c.confirmar();
        return c;
    }

    private Clock relojDentroDelPlazo() {
        return Clock.fixed(FECHA.plusHours(1).atZone(ZONA).toInstant(), ZONA);
    }

    @Test
    void marcaLaDescargaDeUnProductoDigital() {
        assertTrue(useCase.ejecutar(compra.getId(), usuarioId).isDescargado());
        assertTrue(compraRepository.obtenerPorId(compra.getId()).get().isDescargado());
    }

    @Test
    void productoFisicoRechazaYNoMarca() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compraDeFisico.getId(), usuarioId));
        assertFalse(compraRepository.obtenerPorId(compraDeFisico.getId()).get().isDescargado());
    }

    @Test
    void compraPendienteRechazaYNoMarca() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(pendiente.getId(), usuarioId));
        assertFalse(compraRepository.obtenerPorId(pendiente.getId()).get().isDescargado());
    }

    @Test
    void compraAjenaRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(compra.getId(), UUID.randomUUID()));
        assertFalse(compraRepository.obtenerPorId(compra.getId()).get().isDescargado());
    }

    @Test
    void compraInexistenteRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(UUID.randomUUID(), usuarioId));
    }

    @Test
    void unaLineaDePedidoPuedeDescargarse() {
        UUID pedidoId = UUID.randomUUID();
        Compra lineaDePedido = compraCompletada(digital, pedidoId);

        assertTrue(useCase.ejecutar(lineaDePedido.getId(), usuarioId).isDescargado());
    }

    @Test
    void descargarDosVecesNoFallaYSigueDescargada() {
        useCase.ejecutar(compra.getId(), usuarioId);

        assertTrue(useCase.ejecutar(compra.getId(), usuarioId).isDescargado());
    }

    @Test
    void despuesDeDescargarElReembolsoSeRechazaYLaCompraSigueCompletada() {
        useCase.ejecutar(compra.getId(), usuarioId);
        SolicitarReembolsoUseCase reembolso = new SolicitarReembolsoUseCase(
                compraRepository, productoRepository, relojDentroDelPlazo(), TestNotificador.nuevo());

        assertThrows(ReglaDominioException.class, () -> reembolso.ejecutar(compra.getId(), usuarioId));

        assertEquals(EstadoCompra.COMPLETADA, compraRepository.obtenerPorId(compra.getId()).get().getEstado());
    }

    // Review Focus 1: un pedido con una línea descargada no se puede reembolsar completo.
    @Test
    void reembolsoDelPedidoCompletoSeRechazaSiUnaLineaFueDescargada() {
        UUID pedidoId = UUID.randomUUID();
        Compra lineaDigital = compraCompletada(digital, pedidoId);
        Compra lineaFisica = compraCompletada(fisico, pedidoId);
        useCase.ejecutar(lineaDigital.getId(), usuarioId);
        SolicitarReembolsoPedidoUseCase reembolsoPedido = new SolicitarReembolsoPedidoUseCase(
                compraRepository, productoRepository, relojDentroDelPlazo(), TestNotificador.nuevo());

        assertThrows(ReglaDominioException.class, () -> reembolsoPedido.ejecutar(pedidoId, usuarioId));

        assertTrue(compraRepository.obtenerPorPedidoId(pedidoId).stream()
                .allMatch(c -> c.getEstado() == EstadoCompra.COMPLETADA));
        assertEquals(5, productoRepository.obtenerPorId(lineaFisica.getProductoId()).get().getStock());
    }
}
