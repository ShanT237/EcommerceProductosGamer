package com.uniquindio.backend.application.usecase;

import com.uniquindio.backend.domain.entity.*;
import com.uniquindio.backend.domain.exception.ReglaDominioException;
import com.uniquindio.backend.domain.valueobject.EstadoCompra;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ComprarComboUseCaseTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 1);

    private ProductoRepositoryEnMemoria productoRepository;
    private CompraRepositoryEnMemoria compraRepository;
    private UsuarioRepositoryEnMemoria usuarioRepository;
    private VendedorRepositoryEnMemoria vendedorRepository;
    private PreventaRepositoryEnMemoria preventaRepository;
    private ComboRepositoryEnMemoria comboRepository;
    private ComprarComboUseCase useCase;
    private UUID usuarioId;
    private Vendedor vendedor;
    private Producto teclado;
    private Producto mouse;
    private Combo combo;

    @BeforeEach
    void setUp() {
        productoRepository = new ProductoRepositoryEnMemoria();
        compraRepository = new CompraRepositoryEnMemoria();
        usuarioRepository = new UsuarioRepositoryEnMemoria();
        vendedorRepository = new VendedorRepositoryEnMemoria();
        preventaRepository = new PreventaRepositoryEnMemoria();
        comboRepository = new ComboRepositoryEnMemoria();
        ZoneId zona = ZoneId.systemDefault();
        Clock reloj = Clock.fixed(HOY.atTime(10, 0).atZone(zona).toInstant(), zona);
        useCase = new ComprarComboUseCase(comboRepository, productoRepository, compraRepository,
                usuarioRepository, vendedorRepository, preventaRepository, reloj);

        vendedor = Vendedor.registrar(UUID.randomUUID(), "GamerZone", "v@g.com", TipoVendedor.EMPRESA, "900");
        vendedorRepository.guardar(vendedor);
        usuarioId = UUID.randomUUID();
        usuarioRepository.guardar(Usuario.registrar(usuarioId, "Gamer", "g@test.com"));
        teclado = producto("Teclado", 100, 5, Exclusividad.NORMAL);
        mouse = producto("Mouse", 50, 5, Exclusividad.NORMAL);
        combo = Combo.crear(UUID.randomUUID(), vendedor.getId(), "Combo", "Setup", 20.0,
                List.of(teclado.getId(), mouse.getId()));
        comboRepository.guardar(combo);
    }

    private Producto producto(String nombre, double precio, int stock, Exclusividad exclusividad) {
        Producto p = Producto.publicar(UUID.randomUUID(), vendedor.getId(), nombre, "Desc", precio, stock,
                Gama.MEDIA, exclusividad);
        productoRepository.guardar(p);
        return p;
    }

    private void assertNadaCambio() {
        assertEquals(5, teclado.getStock());
        assertEquals(5, mouse.getStock());
        assertTrue(compraRepository.obtenerPorUsuarioId(usuarioId).isEmpty());
    }

    @Test
    void compraTodosLosProductosConElDescuentoDelCombo() {
        List<Compra> compras = useCase.ejecutar(usuarioId, combo.getId(), 2);

        assertEquals(2, compras.size());
        assertTrue(compras.stream().allMatch(c -> c.getEstado() == EstadoCompra.PENDIENTE));
        assertEquals(80, compras.get(0).getPrecioUnitario().valor(), 0.001);
        assertEquals(40, compras.get(1).getPrecioUnitario().valor(), 0.001);
        assertEquals(3, teclado.getStock());
        assertEquals(3, mouse.getStock());
        assertEquals(2, compraRepository.obtenerPorUsuarioId(usuarioId).size());
    }

    @Test
    void sinStockEnUnProductoNoDescuentaNingunoNiGuardaCompras() {
        Producto escaso = producto("Headset", 80, 1, Exclusividad.NORMAL);
        combo.agregarProducto(escaso.getId());

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 2));

        assertNadaCambio();
        assertEquals(1, escaso.getStock());
    }

    @Test
    void productoPausadoOEliminadoRechazaTodoElCombo() {
        mouse.pausar();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertNadaCambio();

        mouse.activar();
        mouse.eliminar(false);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertNadaCambio();
    }

    @Test
    void exclusivoSoloEnCantidadUnoYUnaVezPorUsuario() {
        Producto exclusivo = producto("Edicion", 200, 5, Exclusividad.EXCLUSIVO);
        combo.agregarProducto(exclusivo.getId());

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 2));
        assertNadaCambio();

        useCase.ejecutar(usuarioId, combo.getId(), 1);
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertEquals(4, teclado.getStock());
    }

    @Test
    void preventaAunNoLanzadaRechazaElCombo() {
        preventaRepository.guardar(Preventa.programar(UUID.randomUUID(), mouse.getId(),
                HOY.plusDays(5), "Lanzamiento"));

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertNadaCambio();
    }

    @Test
    void vendedorDadoDeBajaRechazaElCombo() {
        vendedor.darDeBaja();

        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertNadaCambio();
    }

    @Test
    void usuarioInactivoOInexistenteRechaza() {
        usuarioRepository.obtenerPorId(usuarioId).get().desactivar();
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 1));
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(UUID.randomUUID(), combo.getId(), 1));
    }

    @Test
    void cantidadNoPositivaRechaza() {
        assertThrows(ReglaDominioException.class, () -> useCase.ejecutar(usuarioId, combo.getId(), 0));
        assertNadaCambio();
    }

    @Test
    void comboInexistenteLanzaNoSuchElement() {
        assertThrows(NoSuchElementException.class, () -> useCase.ejecutar(usuarioId, UUID.randomUUID(), 1));
    }
}
