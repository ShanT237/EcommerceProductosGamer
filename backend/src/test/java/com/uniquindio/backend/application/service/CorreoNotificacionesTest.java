package com.uniquindio.backend.application.service;

import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import com.uniquindio.backend.domain.valueobject.TipoVendedor;
import com.uniquindio.backend.infrastructure.persistence.UsuarioRepositoryEnMemoria;
import com.uniquindio.backend.infrastructure.persistence.VendedorRepositoryEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CorreoNotificacionesTest {

    private static final List<TipoNotificacion> TIPOS_COMPRADOR = List.of(
            TipoNotificacion.DESCUENTO, TipoNotificacion.STOCK_DISPONIBLE, TipoNotificacion.PREVENTA_DISPONIBLE);
    private static final List<TipoNotificacion> TIPOS_VENDEDOR = List.of(
            TipoNotificacion.STOCK_BAJO, TipoNotificacion.STOCK_AGOTADO, TipoNotificacion.REEMBOLSO_SOLICITADO,
            TipoNotificacion.NUEVA_RESENA, TipoNotificacion.NUEVA_VALORACION);

    private UsuarioRepositoryEnMemoria usuarios;
    private VendedorRepositoryEnMemoria vendedores;
    private List<String[]> correos;
    private CorreoNotificaciones servicio;
    private Usuario comprador;
    private Vendedor vendedor;

    @BeforeEach
    void setUp() {
        usuarios = new UsuarioRepositoryEnMemoria();
        vendedores = new VendedorRepositoryEnMemoria();
        correos = new ArrayList<>();
        servicio = new CorreoNotificaciones(
                (destino, asunto, html) -> correos.add(new String[]{destino, asunto, html}),
                usuarios, vendedores, "https://tienda.test/", true);

        comprador = Usuario.registrar(UUID.randomUUID(), "Ana", "ana@test.com");
        usuarios.guardar(comprador);
        vendedor = Vendedor.registrar(UUID.randomUUID(), "Tienda Gamer", "tienda@test.com",
                TipoVendedor.EMPRESA, "900123456");
        vendedores.guardar(vendedor);
    }

    private static Notificacion aviso(UUID destinatario, TipoNotificacion tipo, String mensaje) {
        return Notificacion.crear(destinatario, null, tipo, mensaje);
    }

    @Test
    void elCompradorRecibeCorreoDeDescuentoStockYPreventa() {
        for (TipoNotificacion tipo : TIPOS_COMPRADOR) {
            servicio.enviar(aviso(comprador.getId(), tipo, "Aviso de prueba"));
        }

        assertEquals(3, correos.size());
        assertEquals(3, correos.stream().map(c -> c[1]).distinct().count());
        for (String[] c : correos) {
            assertEquals("ana@test.com", c[0]);
            assertTrue(c[2].contains("Hola, Ana"));
            assertTrue(c[2].contains("Aviso de prueba"));
            assertTrue(c[2].contains("https://tienda.test/favoritos"));
        }
    }

    @Test
    void elVendedorRecibeCorreoDeStockReembolsoResenaYValoracion() {
        for (TipoNotificacion tipo : TIPOS_VENDEDOR) {
            servicio.enviar(aviso(vendedor.getId(), tipo, "Aviso de prueba"));
        }

        assertEquals(5, correos.size());
        assertEquals(5, correos.stream().map(c -> c[1]).distinct().count());
        for (String[] c : correos) {
            assertEquals("tienda@test.com", c[0]);
            assertTrue(c[2].contains("Hola, Tienda Gamer"));
            assertTrue(c[2].contains("https://tienda.test/vendedor"));
        }
    }

    @Test
    void soloOchoTiposTienenCorreo() {
        Set<TipoNotificacion> conCorreo = Arrays.stream(TipoNotificacion.values())
                .filter(CorreoNotificaciones::enviaCorreo).collect(Collectors.toSet());

        assertEquals(Set.copyOf(
                java.util.stream.Stream.concat(TIPOS_COMPRADOR.stream(), TIPOS_VENDEDOR.stream()).toList()),
                conCorreo);
    }

    @Test
    void losTiposSinPlantillaNoEnvianCorreo() {
        for (TipoNotificacion tipo : List.of(TipoNotificacion.NUEVA_VENTA, TipoNotificacion.META_ALCANZADA,
                TipoNotificacion.REGALO_ENVIADO, TipoNotificacion.REGALO_CANCELADO, TipoNotificacion.REGALO_ELEGIDO)) {
            servicio.enviar(aviso(comprador.getId(), tipo, "x"));
            servicio.enviar(aviso(vendedor.getId(), tipo, "x"));
        }

        assertTrue(correos.isEmpty());
    }

    @Test
    void unTipoDeVendedorDirigidoAUnCompradorNoSeEnvia() {
        servicio.enviar(aviso(comprador.getId(), TipoNotificacion.STOCK_BAJO, "x"));
        servicio.enviar(aviso(vendedor.getId(), TipoNotificacion.DESCUENTO, "x"));

        assertTrue(correos.isEmpty());
    }

    @Test
    void unaCuentaInactivaNoRecibeCorreo() {
        comprador.desactivar();
        vendedor.darDeBaja();

        servicio.enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x"));
        servicio.enviar(aviso(vendedor.getId(), TipoNotificacion.STOCK_BAJO, "x"));

        assertTrue(correos.isEmpty());
    }

    @Test
    void unDestinatarioInexistenteNoFalla() {
        assertDoesNotThrow(() -> servicio.enviar(aviso(UUID.randomUUID(), TipoNotificacion.DESCUENTO, "x")));
        assertTrue(correos.isEmpty());
    }

    @Test
    void apagadoPorConfiguracionNoEnvia() {
        CorreoNotificaciones apagado = new CorreoNotificaciones(
                (d, a, h) -> correos.add(new String[]{d, a, h}), usuarios, vendedores, "https://tienda.test", false);

        apagado.enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x"));

        assertTrue(correos.isEmpty());
    }

    @Test
    void sinEnvioNoHaceNada() {
        assertDoesNotThrow(() -> CorreoNotificaciones.sinEnvio()
                .enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x")));
    }

    @Test
    void escapaElHtmlDelMensajeYDelNombre() {
        Usuario travieso = Usuario.registrar(UUID.randomUUID(), "<i>Ana</i>", "t@test.com");
        usuarios.guardar(travieso);

        servicio.enviar(aviso(travieso.getId(), TipoNotificacion.DESCUENTO, "El producto '<b>X</b>' bajó"));

        String html = correos.get(0)[2];
        assertTrue(html.contains("&lt;b&gt;X&lt;/b&gt;"));
        assertFalse(html.contains("<b>X</b>"));
        assertFalse(html.contains("<i>Ana</i>"));
    }

    @Test
    void siElEnviadorFallaNoSePropagaLaExcepcion() {
        CorreoNotificaciones roto = new CorreoNotificaciones(
                (d, a, h) -> { throw new IllegalStateException("proveedor caído"); },
                usuarios, vendedores, "https://tienda.test", true);

        assertDoesNotThrow(() -> roto.enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x")));
    }

    @Test
    void conTransaccionActivaElCorreoSaleSoloTrasElCommit() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            servicio.enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x"));
            assertTrue(correos.isEmpty());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
            assertEquals(1, correos.size());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void siLaTransaccionSeRevierteNoSeEnviaNada() {
        TransactionSynchronizationManager.initSynchronization();
        try {
            servicio.enviar(aviso(comprador.getId(), TipoNotificacion.DESCUENTO, "x"));
            // rollback: Spring no invoca afterCommit
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertTrue(correos.isEmpty());
    }
}
