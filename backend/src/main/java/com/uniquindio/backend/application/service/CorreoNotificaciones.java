package com.uniquindio.backend.application.service;

import com.uniquindio.backend.application.port.out.EnviadorCorreos;
import com.uniquindio.backend.domain.entity.Notificacion;
import com.uniquindio.backend.domain.entity.Usuario;
import com.uniquindio.backend.domain.entity.Vendedor;
import com.uniquindio.backend.domain.repository.UsuarioRepository;
import com.uniquindio.backend.domain.repository.VendedorRepository;
import com.uniquindio.backend.domain.valueobject.Rol;
import com.uniquindio.backend.domain.valueobject.TipoNotificacion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.util.HtmlUtils;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Reenvía por correo algunas notificaciones, reutilizando el mismo puerto {@link EnviadorCorreos}
 * que ya usa la recuperación de contraseña.
 *
 * Solo se envía correo de los tipos que tienen plantilla (ver {@link #PLANTILLAS}):
 *  - Comprador: DESCUENTO, STOCK_DISPONIBLE y PREVENTA_DISPONIBLE.
 *  - Vendedor: STOCK_BAJO, STOCK_AGOTADO, REEMBOLSO_SOLICITADO, NUEVA_RESENA y NUEVA_VALORACION.
 * El resto de notificaciones (nueva venta, meta alcanzada, regalos...) siguen siendo solo internas.
 *
 * Es de "mejor esfuerzo": nunca lanza excepciones al llamador. Si hay una transacción en curso, el
 * correo sale únicamente cuando esta se confirma (si se revierte, no se manda un aviso de algo que
 * no ocurrió). Se puede apagar con app.notificaciones.correo-habilitado=false.
 */
@Service
public class CorreoNotificaciones {

    private static final Logger log = LoggerFactory.getLogger(CorreoNotificaciones.class);

    private record Plantilla(Rol destinatario, String asunto, String titulo, String ruta, String textoBoton) {}

    private record Destino(String nombre, String correo) {}

    private static final Map<TipoNotificacion, Plantilla> PLANTILLAS = new EnumMap<>(TipoNotificacion.class);

    static {
        // Comprador
        PLANTILLAS.put(TipoNotificacion.DESCUENTO, new Plantilla(Rol.COMPRADOR,
                "Bajó el precio de un producto de tu lista de deseos · GamerZone",
                "¡Un producto de tu lista de deseos bajó de precio!", "/favoritos", "Ver mi lista de deseos"));
        PLANTILLAS.put(TipoNotificacion.STOCK_DISPONIBLE, new Plantilla(Rol.COMPRADOR,
                "Un producto de tu lista de deseos volvió a tener stock · GamerZone",
                "¡Volvió a tener stock!", "/favoritos", "Ver mi lista de deseos"));
        PLANTILLAS.put(TipoNotificacion.PREVENTA_DISPONIBLE, new Plantilla(Rol.COMPRADOR,
                "Ya está disponible una preventa de tu lista de deseos · GamerZone",
                "¡Tu preventa ya está disponible!", "/favoritos", "Ver mi lista de deseos"));
        // Vendedor
        PLANTILLAS.put(TipoNotificacion.STOCK_BAJO, new Plantilla(Rol.VENDEDOR,
                "Stock bajo en uno de tus productos · GamerZone",
                "Stock bajo", "/vendedor", "Ir a mi panel"));
        PLANTILLAS.put(TipoNotificacion.STOCK_AGOTADO, new Plantilla(Rol.VENDEDOR,
                "Se agotó uno de tus productos · GamerZone",
                "Producto agotado", "/vendedor", "Ir a mi panel"));
        PLANTILLAS.put(TipoNotificacion.REEMBOLSO_SOLICITADO, new Plantilla(Rol.VENDEDOR,
                "Solicitud de reembolso en uno de tus productos · GamerZone",
                "Reembolso solicitado", "/vendedor", "Ir a mi panel"));
        PLANTILLAS.put(TipoNotificacion.NUEVA_RESENA, new Plantilla(Rol.VENDEDOR,
                "Nueva reseña en uno de tus productos · GamerZone",
                "Nueva reseña", "/vendedor", "Ir a mi panel"));
        PLANTILLAS.put(TipoNotificacion.NUEVA_VALORACION, new Plantilla(Rol.VENDEDOR,
                "Un comprador te valoró · GamerZone",
                "Nueva valoración", "/vendedor", "Ir a mi panel"));
    }

    private final EnviadorCorreos enviadorCorreos;
    private final UsuarioRepository usuarioRepository;
    private final VendedorRepository vendedorRepository;
    private final String urlFrontend;
    private final boolean habilitado;

    @Autowired
    public CorreoNotificaciones(
            EnviadorCorreos enviadorCorreos,
            UsuarioRepository usuarioRepository,
            VendedorRepository vendedorRepository,
            @Value("${app.frontend-url:http://localhost:4200}") String urlFrontend,
            @Value("${app.notificaciones.correo-habilitado:true}") boolean habilitado) {
        this.enviadorCorreos = enviadorCorreos;
        this.usuarioRepository = usuarioRepository;
        this.vendedorRepository = vendedorRepository;
        String url = urlFrontend == null ? "" : urlFrontend;
        this.urlFrontend = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        this.habilitado = habilitado;
    }

    /** Instancia que nunca envía nada: para quien arma un notificador sin correo (p. ej. pruebas unitarias). */
    public static CorreoNotificaciones sinEnvio() {
        return new CorreoNotificaciones(null, null, null, "", false);
    }

    /** Indica si este tipo de notificación también se manda por correo. */
    public static boolean enviaCorreo(TipoNotificacion tipo) {
        return PLANTILLAS.containsKey(tipo);
    }

    /** Manda el correo de la notificación, si su tipo lo tiene y la cuenta destinataria está activa. */
    public void enviar(Notificacion notificacion) {
        if (!habilitado || notificacion == null) {
            return;
        }
        Plantilla plantilla = PLANTILLAS.get(notificacion.getTipo());
        if (plantilla == null) {
            return;
        }
        try {
            // Se resuelve el destinatario ya (dentro de la transacción); solo el envío espera al commit.
            Optional<Destino> destino = resolverDestino(notificacion, plantilla);
            if (destino.isEmpty()) {
                return;
            }
            String asunto = plantilla.asunto();
            String html = plantillaCorreo(plantilla, destino.get().nombre(), notificacion.getMensaje());
            String correo = destino.get().correo();
            despacharTrasCommit(() -> enviadorCorreos.enviar(correo, asunto, html));
        } catch (RuntimeException e) {
            log.warn("No se pudo preparar el correo de la notificación {}: {}", notificacion.getTipo(), e.getMessage());
        }
    }

    private Optional<Destino> resolverDestino(Notificacion notificacion, Plantilla plantilla) {
        Optional<Destino> destino = plantilla.destinatario().esComprador()
                ? usuarioRepository.obtenerPorId(notificacion.getUsuarioId())
                        .filter(Usuario::isActivo)
                        .map(u -> new Destino(u.getNombre(), u.getCorreo()))
                : vendedorRepository.obtenerPorId(notificacion.getUsuarioId())
                        .filter(Vendedor::isActivo)
                        .map(v -> new Destino(v.getNombre(), v.getCorreo()));
        return destino.filter(d -> d.correo() != null && !d.correo().isBlank());
    }

    private void despacharTrasCommit(Runnable envio) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ejecutarSinFallar(envio);
                }
            });
        } else {
            ejecutarSinFallar(envio);
        }
    }

    private static void ejecutarSinFallar(Runnable envio) {
        try {
            envio.run();
        } catch (RuntimeException e) {
            log.warn("Falló el envío del correo de notificación: {}", e.getMessage());
        }
    }

    private String plantillaCorreo(Plantilla plantilla, String nombre, String mensaje) {
        String enlace = HtmlUtils.htmlEscape(urlFrontend + plantilla.ruta());
        String enlaceNotificaciones = HtmlUtils.htmlEscape(urlFrontend + "/notificaciones");
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:auto;padding:24px;color:#1c1b29">
                <h2 style="margin:0 0 12px">%s</h2>
                <p>Hola, %s.</p>
                <p>%s</p>
                <p style="margin:24px 0"><a href="%s" style="background:#22e4ff;color:#07060f;padding:12px 22px;border-radius:8px;text-decoration:none;font-weight:bold">%s</a></p>
                <p style="font-size:13px;color:#555">Este aviso también aparece en tus notificaciones de GamerZone: <a href="%s">%s</a></p>
                </div>
                """.formatted(
                HtmlUtils.htmlEscape(plantilla.titulo()),
                HtmlUtils.htmlEscape(nombre == null ? "" : nombre),
                HtmlUtils.htmlEscape(mensaje == null ? "" : mensaje),
                enlace,
                HtmlUtils.htmlEscape(plantilla.textoBoton()),
                enlaceNotificaciones,
                enlaceNotificaciones);
    }
}
