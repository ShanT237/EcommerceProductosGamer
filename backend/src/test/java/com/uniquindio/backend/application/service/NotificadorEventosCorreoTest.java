package com.uniquindio.backend.application.service;

import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica que el notificador, además de guardar el aviso, lo manda por correo cuando corresponde. */
class NotificadorEventosCorreoTest {

    private NotificacionRepositoryEnMemoria notificaciones;
    private WishlistRepositoryEnMemoria wishlists;
    private List<String[]> correos;
    private NotificadorEventos notificador;
    private Usuario comprador;
    private Vendedor vendedor;
    private Producto producto;

    @BeforeEach
    void setUp() {
        notificaciones = new NotificacionRepositoryEnMemoria();
        wishlists = new WishlistRepositoryEnMemoria();
        UsuarioRepositoryEnMemoria usuarios = new UsuarioRepositoryEnMemoria();
        VendedorRepositoryEnMemoria vendedores = new VendedorRepositoryEnMemoria();
        correos = new ArrayList<>();
        notificador = new NotificadorEventos(notificaciones, wishlists, new CorreoNotificaciones(
                (d, a, h) -> correos.add(new String[]{d, a, h}), usuarios, vendedores, "https://tienda.test", true));

        comprador = Usuario.registrar(UUID.randomUUID(), "Ana", "ana@test.com");
        usuarios.guardar(comprador);
        vendedor = Vendedor.registrar(UUID.randomUUID(), "Tienda Gamer", "tienda@test.com",
                TipoVendedor.EMPRESA, "900123456");
        vendedores.guardar(vendedor);

        producto = Producto.publicar(UUID.randomUUID(), vendedor.getId(), "Teclado RGB", "Mecánico",
                120.0, 10, Gama.ALTA, Exclusividad.NORMAL);
        Wishlist w = Wishlist.crear(UUID.randomUUID(), comprador.getId());
        w.agregarProducto(producto.getId());
        wishlists.guardar(w);
    }

    @Test
    void stockRepuestoEnviaCorreoAlCompradorSoloSiEstabaAgotado() {
        notificador.stockRepuesto(producto, 3);
        assertTrue(correos.isEmpty());

        notificador.stockRepuesto(producto, 0);
        assertEquals(1, correos.size());
        assertEquals("ana@test.com", correos.get(0)[0]);
        assertTrue(correos.get(0)[2].contains("Teclado RGB"));
    }

    @Test
    void preventaDisponibleEnviaUnSoloCorreoPorUsuario() {
        Preventa preventa = Preventa.reconstruir(UUID.randomUUID(), producto.getId(), LocalDate.now(), "Edición");

        notificador.preventaDisponible(producto, preventa);
        notificador.preventaDisponible(producto, preventa);

        assertEquals(1, correos.size());
        assertEquals("ana@test.com", correos.get(0)[0]);
    }

    @Test
    void stockBajoYAgotadoEnvianCorreoAlVendedor() {
        producto.reducirStock(5);
        notificador.stockDelVendedor(producto, 10);
        producto.reducirStock(5);
        notificador.stockDelVendedor(producto, 5);

        assertEquals(2, correos.size());
        correos.forEach(c -> assertEquals("tienda@test.com", c[0]));
    }

    @Test
    void reembolsoResenaYValoracionEnvianCorreoPeroLaVentaNo() {
        notificador.nuevaVenta(producto, 2);
        assertTrue(correos.isEmpty());

        notificador.reembolsoSolicitado(producto, 1);
        notificador.nuevaResena(producto, 5);
        notificador.nuevaValoracion(producto, 4);

        assertEquals(3, correos.size());
        correos.forEach(c -> assertEquals("tienda@test.com", c[0]));
        assertEquals(4, notificaciones.obtenerPorUsuarioId(vendedor.getId()).size());
    }

    @Test
    void metaYRegalosSoloGuardanLaNotificacionSinCorreo() {
        UUID usuarioId = comprador.getId();
        notificador.metaAlcanzada(usuarioId, Usuario.COMPRAS_POR_REGALO - 1, Usuario.COMPRAS_POR_REGALO);

        Regalo enviado = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        enviado.elegir(usuarioId, false);
        enviado.enviar();
        notificador.regaloEnviado(enviado);

        Regalo elegido = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        elegido.elegir(usuarioId, false);
        notificador.regaloElegido(vendedor.getId(), elegido, "Teclado RGB");

        Regalo cancelado = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        cancelado.elegir(usuarioId, false);
        cancelado.cancelarRegalo();
        notificador.regaloCancelado(cancelado, "Teclado RGB");

        assertTrue(correos.isEmpty());
        assertEquals(3, notificaciones.obtenerPorUsuarioId(usuarioId).size());
        assertEquals(1, notificaciones.obtenerPorUsuarioId(vendedor.getId()).size());
    }

    @Test
    void siElCorreoFallaLaNotificacionInternaSeGuardaIgual() {
        NotificadorEventos roto = new NotificadorEventos(notificaciones, wishlists, new CorreoNotificaciones(
                (d, a, h) -> { throw new IllegalStateException("proveedor caído"); },
                new UsuarioRepositoryEnMemoria(), new VendedorRepositoryEnMemoria(), "https://tienda.test", true));

        assertDoesNotThrow(() -> roto.nuevaResena(producto, 5));
        assertEquals(1, notificaciones.obtenerPorUsuarioId(vendedor.getId()).size());
    }
}
