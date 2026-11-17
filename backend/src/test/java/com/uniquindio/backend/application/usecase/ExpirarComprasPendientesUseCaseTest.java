package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.Precio;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ExpirarComprasPendientesUseCaseTest {

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 1, 10, 0);

    private CompraRepositoryEnMemoria compraRepository;
    private ProductoRepositoryEnMemoria productoRepository;
    private NotificacionRepositoryEnMemoria notificaciones;
    private WishlistRepositoryEnMemoria wishlists;
    private ExpirarComprasPendientesUseCase useCase;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        compraRepository = new CompraRepositoryEnMemoria();
        productoRepository = new ProductoRepositoryEnMemoria();
        notificaciones = new NotificacionRepositoryEnMemoria();
        wishlists = new WishlistRepositoryEnMemoria();
        Clock reloj = Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
        useCase = new ExpirarComprasPendientesUseCase(compraRepository, productoRepository, reloj,
                TestNotificador.con(notificaciones, wishlists));
        usuarioId = UUID.randomUUID();
    }

    /** Producto cuyo stock ya fue descontado por la compra: queda {@code stockInicial - descontado}. */
    private Producto producto(String nombre, int stockInicial, int descontado) {
        Producto p = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), nombre, "Desc", 100,
                stockInicial, Gama.MEDIA, Exclusividad.NORMAL);
        if (descontado > 0) {
            p.reducirStock(descontado);
        }
        productoRepository.guardar(p);
        return p;
    }

    private Compra compra(Producto producto, int cantidad, LocalDateTime fecha, UUID pedidoId) {
        Compra c = Compra.realizar(UUID.randomUUID(), usuarioId, producto.getId(), cantidad, fecha,
                new Precio(producto.getPrecio()), pedidoId);
        compraRepository.guardar(c);
        return c;
    }

    private EstadoCompra estado(Compra c) {
        return compraRepository.obtenerPorId(c.getId()).orElseThrow().getEstado();
    }

    @Test
    void expiraLaCompraPendienteDeMasDeDiezMinutosYRestituyeElStock() {
        Producto producto = producto("Teclado", 5, 2);
        Compra vieja = compra(producto, 2, AHORA.minusMinutes(11), null);

        assertEquals(1, useCase.ejecutar());

        assertEquals(EstadoCompra.EXPIRADA, estado(vieja));
        assertEquals(5, productoRepository.obtenerPorId(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void laPendienteDeMenosDeDiezMinutosNoSeToca() {
        Producto producto = producto("Teclado", 5, 2);
        Compra reciente = compra(producto, 2, AHORA.minusMinutes(9), null);

        assertEquals(0, useCase.ejecutar());

        assertEquals(EstadoCompra.PENDIENTE, estado(reciente));
        assertEquals(3, productoRepository.obtenerPorId(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void aExactamenteDiezMinutosNoExpira() {
        Producto producto = producto("Teclado", 5, 2);
        Compra justa = compra(producto, 2, AHORA.minusMinutes(10), null);

        assertEquals(0, useCase.ejecutar());

        assertEquals(EstadoCompra.PENDIENTE, estado(justa));
    }

    @Test
    void expiraElPedidoCompletoYRestituyeElStockDeCadaLinea() {
        UUID pedidoId = UUID.randomUUID();
        Producto teclado = producto("Teclado", 5, 2);
        Producto mouse = producto("Mouse", 5, 1);
        compra(teclado, 2, AHORA.minusMinutes(11), pedidoId);
        compra(mouse, 1, AHORA.minusMinutes(1), pedidoId);   // la otra línea aún no vencía por sí sola

        assertEquals(2, useCase.ejecutar());

        assertTrue(compraRepository.obtenerPorPedidoId(pedidoId).stream()
                .allMatch(c -> c.getEstado() == EstadoCompra.EXPIRADA));
        assertEquals(5, productoRepository.obtenerPorId(teclado.getId()).orElseThrow().getStock());
        assertEquals(5, productoRepository.obtenerPorId(mouse.getId()).orElseThrow().getStock());
    }

    @Test
    void dosLineasDelMismoProductoRestituyenAmbasCantidades() {
        UUID pedidoId = UUID.randomUUID();
        Producto teclado = producto("Teclado", 5, 3);
        compra(teclado, 2, AHORA.minusMinutes(11), pedidoId);
        compra(teclado, 1, AHORA.minusMinutes(11), pedidoId);

        assertEquals(2, useCase.ejecutar());

        assertEquals(5, productoRepository.obtenerPorId(teclado.getId()).orElseThrow().getStock());
    }

    // Review Focus 3: un pedido con una línea ya COMPLETADA expira solo las PENDIENTES.
    @Test
    void unPedidoConUnaLineaYaCompletadaExpiraSoloLasPendientes() {
        UUID pedidoId = UUID.randomUUID();
        Producto teclado = producto("Teclado", 5, 2);
        Producto mouse = producto("Mouse", 5, 1);
        Compra lineaCompletada = compra(teclado, 2, AHORA.minusMinutes(30), pedidoId);
        lineaCompletada.confirmar();
        Compra lineaPendiente = compra(mouse, 1, AHORA.minusMinutes(30), pedidoId);

        assertDoesNotThrow(() -> useCase.ejecutar());

        assertEquals(EstadoCompra.COMPLETADA, estado(lineaCompletada));
        assertEquals(EstadoCompra.EXPIRADA, estado(lineaPendiente));
        assertEquals(3, productoRepository.obtenerPorId(teclado.getId()).orElseThrow().getStock());
        assertEquals(5, productoRepository.obtenerPorId(mouse.getId()).orElseThrow().getStock());
    }

    @Test
    void noTocaComprasCompletadasNiCanceladas() {
        Producto producto = producto("Teclado", 5, 2);
        Compra completada = compra(producto, 1, AHORA.minusHours(3), null);
        completada.confirmar();
        Compra cancelada = compra(producto, 1, AHORA.minusHours(3), null);
        cancelada.cancelar();

        assertEquals(0, useCase.ejecutar());

        assertEquals(EstadoCompra.COMPLETADA, estado(completada));
        assertEquals(EstadoCompra.CANCELADA, estado(cancelada));
        assertEquals(3, productoRepository.obtenerPorId(producto.getId()).orElseThrow().getStock());
    }

    @Test
    void sinPendientesDevuelveCero() {
        assertEquals(0, useCase.ejecutar());
    }

    @Test
    void avisaALaWishlistSiElProductoVuelveATenerStock() {
        Producto producto = producto("Teclado", 1, 1);        // agotado
        compra(producto, 1, AHORA.minusMinutes(11), null);
        UUID wishUsuario = UUID.randomUUID();
        Wishlist w = Wishlist.crear(UUID.randomUUID(), wishUsuario);
        w.agregarProducto(producto.getId());
        wishlists.guardar(w);

        useCase.ejecutar();

        assertEquals(TipoNotificacion.STOCK_DISPONIBLE,
                notificaciones.obtenerPorUsuarioId(wishUsuario).get(0).getTipo());
    }

    @Test
    void unGrupoConProductoInexistenteSeOmiteSinMutarloYNoBloqueaLosDemas() {
        Producto fantasma = Producto.publicar(UUID.randomUUID(), UUID.randomUUID(), "Fantasma", "Desc", 100, 5,
                Gama.MEDIA, Exclusividad.NORMAL);              // nunca se guarda en el repositorio
        Compra sinProducto = compra(fantasma, 1, AHORA.minusMinutes(30), null);
        Producto teclado = producto("Teclado", 5, 2);
        Compra normal = compra(teclado, 2, AHORA.minusMinutes(30), null);

        assertEquals(1, useCase.ejecutar());

        assertEquals(EstadoCompra.PENDIENTE, estado(sinProducto));
        assertEquals(EstadoCompra.EXPIRADA, estado(normal));
    }
}
