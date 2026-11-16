package com.uniquindio.backend.application.service;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Preventa;
import com.uniquindio.backend.domain.entity.Producto;
import com.uniquindio.backend.domain.entity.Regalo;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Wishlist;
import com.uniquindio.backend.domain.valueobject.Exclusividad;
import com.uniquindio.backend.domain.valueobject.Gama;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.infrastructure.persistence.NotificacionRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.WishlistRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificadorEventosTest {

    private NotificacionRepositoryEnMemoria notificaciones;
    private WishlistRepositoryEnMemoria wishlists;
    private NotificadorEventos notificador;
    private UUID vendedorId;
    private Producto producto;

    @BeforeEach
    void setUp() {
        notificaciones = new NotificacionRepositoryEnMemoria();
        wishlists = new WishlistRepositoryEnMemoria();
        notificador = new NotificadorEventos(notificaciones, wishlists);
        vendedorId = UUID.randomUUID();
        producto = Producto.publicar(UUID.randomUUID(), vendedorId, "Teclado RGB", "Mecánico",
                120.0, 10, Gama.ALTA, Exclusividad.NORMAL);
    }

    private UUID compradorConProductoEnWishlist() {
        UUID usuarioId = UUID.randomUUID();
        Wishlist w = Wishlist.crear(UUID.randomUUID(), usuarioId);
        w.agregarProducto(producto.getId());
        wishlists.guardar(w);
        return usuarioId;
    }

    private Notificacion unica(UUID destinatario, TipoNotificacion tipo) {
        List<Notificacion> lista = notificaciones.obtenerPorUsuarioId(destinatario);
        assertEquals(1, lista.size());
        assertEquals(tipo, lista.get(0).getTipo());
        return lista.get(0);
    }

    // ---------------------------------------------------------------- comprador

    @Test
    void stockRepuestoAvisaALaWishlistSoloSiEstabaAgotado() {
        UUID comprador = compradorConProductoEnWishlist();

        notificador.stockRepuesto(producto, 3);               // ya había stock: no es reposición
        assertTrue(notificaciones.obtenerPorUsuarioId(comprador).isEmpty());

        notificador.stockRepuesto(producto, 0);               // estaba agotado y ahora hay 10
        unica(comprador, TipoNotificacion.STOCK_DISPONIBLE);
    }

    @Test
    void preventaDisponibleNoSeRepiteParaElMismoUsuario() {
        UUID comprador = compradorConProductoEnWishlist();
        Preventa preventa = Preventa.reconstruir(UUID.randomUUID(), producto.getId(), LocalDate.now(), "Edición limitada");

        notificador.preventaDisponible(producto, preventa);
        notificador.preventaDisponible(producto, preventa);

        unica(comprador, TipoNotificacion.PREVENTA_DISPONIBLE);
    }

    @Test
    void metaAlcanzadaSoloCuandoSeCruzaUnMultiploDeLaMeta() {
        UUID usuarioId = UUID.randomUUID();
        int meta = Usuario.COMPRAS_POR_REGALO;

        notificador.metaAlcanzada(usuarioId, 1, meta - 1);    // aún no llega
        assertTrue(notificaciones.obtenerPorUsuarioId(usuarioId).isEmpty());

        notificador.metaAlcanzada(usuarioId, meta - 1, meta); // la cruza
        Notificacion n = unica(usuarioId, TipoNotificacion.META_ALCANZADA);
        assertNull(n.getProductoId());

        notificador.metaAlcanzada(usuarioId, meta, meta + 1); // ya pasó, no repite
        assertEquals(1, notificaciones.obtenerPorUsuarioId(usuarioId).size());
    }

    @Test
    void regaloEnviadoAvisaAlDestinatario() {
        UUID usuarioId = UUID.randomUUID();
        Regalo regalo = Regalo.crear(UUID.randomUUID(), producto.getId(), null);
        regalo.elegir(usuarioId, false);
        regalo.enviar();

        notificador.regaloEnviado(regalo);

        unica(usuarioId, TipoNotificacion.REGALO_ENVIADO);
    }

    // ----------------------------------------------------------------- vendedor

    @Test
    void stockBajoSoloAlCruzarElUmbral() {
        notificador.stockDelVendedor(producto, 10);           // sigue en 10: nada
        assertTrue(notificaciones.obtenerPorUsuarioId(vendedorId).isEmpty());

        producto.reducirStock(5);                             // 10 -> 5: cruza el umbral
        notificador.stockDelVendedor(producto, 10);
        unica(vendedorId, TipoNotificacion.STOCK_BAJO);

        producto.reducirStock(1);                             // 5 -> 4: ya estaba bajo, no repite
        notificador.stockDelVendedor(producto, 5);
        assertEquals(1, notificaciones.obtenerPorUsuarioId(vendedorId).size());
    }

    @Test
    void stockAgotadoAvisaAlLlegarACero() {
        producto.reducirStock(10);

        notificador.stockDelVendedor(producto, 10);

        unica(vendedorId, TipoNotificacion.STOCK_AGOTADO);
    }

    @Test
    void ventaReembolsoResenaYValoracionLlegaAlVendedor() {
        notificador.nuevaVenta(producto, 2);
        notificador.reembolsoSolicitado(producto, 1);
        notificador.nuevaResena(producto, 5);
        notificador.nuevaValoracion(producto, 4);

        List<TipoNotificacion> tipos = notificaciones.obtenerPorUsuarioId(vendedorId).stream()
                .map(Notificacion::getTipo).toList();
        assertEquals(4, tipos.size());
        assertTrue(tipos.containsAll(List.of(TipoNotificacion.NUEVA_VENTA, TipoNotificacion.REEMBOLSO_SOLICITADO,
                TipoNotificacion.NUEVA_RESENA, TipoNotificacion.NUEVA_VALORACION)));
    }
}
