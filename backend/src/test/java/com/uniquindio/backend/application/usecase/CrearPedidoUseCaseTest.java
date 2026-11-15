package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.Compra;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Promocion;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.CompraRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PreventaRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.ProductoRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.PromocionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CrearPedidoUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private PromocionRepositoryEnMemoria promocionRepository;
    private CrearPedidoUseCase useCase;
    private UUID usuarioId;
    private Vendedor vendedor;
    private Producto teclado;
    private Producto mouse;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        promocionRepository = new PromocionRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new CrearPedidoUseCase(productoRepository, compraRepository, usuarioRepository,
                vendedorRepository, preventaRepository, promocionRepository, reloj);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "g@test.com"));
        teclado = producto("Teclado", 100, 5, Exclusividad.NORMAL);
        mouse = producto("Mouse", 50, 5, Exclusividad.NORMAL);
    }

    private Producto producto(String nombre, double precio, int stock, Exclusividad exclusividad) {
        Producto p = Producto.publicar(UUID.randomUUID(), vendedor.getId(), nombre, "Desc", precio, stock,
                Gama.MEDIA, exclusividad);
        productoRepository.guardar(p);
        return p;
    }

    private List<Compra> crear(ItemPedido... items) {
        return useCase.ejecutar(usuarioId, List.of(items));
    }

    /** El pedido se rechazó entero: ni stock tocado ni compras guardadas. */
    private void assertNadaCambio() {
        assertEquals(5, teclado.getStock());
        assertEquals(5, mouse.getStock());
        assertTrue(compraRepository.obtenerPorUsuarioId(usuarioId).isEmpty());
    }

    @Test
    void creaUnaCompraPendientePorItemConElMismoPedidoId() {
        List<Compra> compras = crear(new ItemPedido(teclado.getId(), 2), new ItemPedido(mouse.getId(), 1));

        assertEquals(2, compras.size());
        assertTrue(compras.stream().allMatch(c -> c.getEstado() == EstadoCompra.PENDIENTE));
        UUID pedidoId = compras.get(0).getPedidoId();
        assertNotNull(pedidoId);
        assertTrue(compras.stream().allMatch(c -> pedidoId.equals(c.getPedidoId())));
        assertEquals(teclado.getId(), compras.get(0).getProductoId());
        assertEquals(2, compras.get(0).getCantidad());
        assertEquals(100, compras.get(0).getPrecioUnitario().valor(), 0.001);
        assertEquals(mouse.getId(), compras.get(1).getProductoId());
        assertEquals(1, compras.get(1).getCantidad());
        assertEquals(50, compras.get(1).getPrecioUnitario().valor(), 0.001);

        Set<UUID> guardadas = compraRepository.obtenerPorPedidoId(pedidoId).stream()
                .map(Compra::getId).collect(Collectors.toSet());
        assertEquals(compras.stream().map(Compra::getId).collect(Collectors.toSet()), guardadas);
    }

    @Test
    void cadaPedidoRecibeUnPedidoIdPropio() {
        UUID primero = crear(new ItemPedido(teclado.getId(), 1)).get(0).getPedidoId();
        UUID segundo = crear(new ItemPedido(mouse.getId(), 1)).get(0).getPedidoId();

        assertNotEquals(primero, segundo);
    }

    @Test
    void descuentaElStockDeTodosLosItems() {
        crear(new ItemPedido(teclado.getId(), 2), new ItemPedido(mouse.getId(), 4));

        assertEquals(3, teclado.getStock());
        assertEquals(1, mouse.getStock());
    }

    @Test
    void siUnItemNoTieneStockNoDescuentaNingunoNiGuardaCompras() {
        // El último ítem es el que falla: los anteriores no deben haberse tocado
        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 2), new ItemPedido(mouse.getId(), 6)));

        assertNadaCambio();
    }

    @Test
    void rechazaElMismoProductoRepetido() {
        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(teclado.getId(), 2)));

        assertNadaCambio();
    }

    @Test
    void rechazaListaVaciaONula() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, List.of()));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, null));

        assertNadaCambio();
    }

    @Test
    void rechazaCantidadNoPositivaEnCualquierItem() {
        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(mouse.getId(), 0)));
        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), -1)));

        assertNadaCambio();
    }

    @Test
    void productoInexistenteRechazaTodoElPedido() {
        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(UUID.randomUUID(), 1)));

        assertNadaCambio();
    }

    @Test
    void aplicaLaPromocionVigente() {
        promocionRepository.guardar(Promocion.programar(UUID.randomUUID(), teclado.getId(),
                20, HOY, HOY.plusDays(9)));

        List<Compra> compras = crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(mouse.getId(), 1));

        assertEquals(80, compras.get(0).getPrecioUnitario().valor(), 0.001);
        assertEquals(50, compras.get(1).getPrecioUnitario().valor(), 0.001);
        assertEquals(100, teclado.getPrecio());
    }

    @Test
    void exclusivoConCantidadMayorAUnoRechaza() {
        Producto exclusivo = producto("Edicion", 200, 5, Exclusividad.EXCLUSIVO);

        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(exclusivo.getId(), 2)));

        assertNadaCambio();
        assertEquals(5, exclusivo.getStock());
    }

    @Test
    void exclusivoYaCompradoPorElUsuarioRechazaTodoElPedido() {
        Producto exclusivo = producto("Edicion", 200, 5, Exclusividad.EXCLUSIVO);
        crear(new ItemPedido(exclusivo.getId(), 1));

        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(exclusivo.getId(), 1)));

        assertEquals(5, teclado.getStock());
        assertEquals(4, exclusivo.getStock());
    }

    @Test
    void preventaFuturaRechaza() {
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), mouse.getId(),
                HOY.plusDays(5), "Lanzamiento"));

        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(mouse.getId(), 1)));

        assertNadaCambio();
    }

    @Test
    void productoPausadoRechaza() {
        mouse.pausar();

        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(mouse.getId(), 1)));

        assertNadaCambio();
    }

    @Test
    void productoEliminadoRechaza() {
        mouse.eliminar(false);

        assertThrows(ReglaDominioException.class,
                () -> crear(new ItemPedido(teclado.getId(), 1), new ItemPedido(mouse.getId(), 1)));

        assertNadaCambio();
    }

    @Test
    void vendedorDadoDeBajaRechaza() {
        vendedor.darDeBaja();

        assertThrows(ReglaDominioException.class, () -> crear(new ItemPedido(teclado.getId(), 1)));

        assertNadaCambio();
    }

    @Test
    void usuarioInactivoRechaza() {
        usuarioRepository.obtenerPorId(usuarioId).get().desactivar();

        assertThrows(ReglaDominioException.class, () -> crear(new ItemPedido(teclado.getId(), 1)));

        assertNadaCambio();
    }

    @Test
    void usuarioInexistenteRechaza() {
        assertThrows(ReglaDominioException.class,
                () -> useCase.ejecutar(UUID.randomUUID(), List.of(new ItemPedido(teclado.getId(), 1))));

        assertNadaCambio();
    }
}
